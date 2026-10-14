package vn.sunicoffee.support;

import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.UserRepository;

@Service
@Transactional(readOnly=true)
public class SupportService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    public SupportService(JdbcTemplate jdbc, UserRepository users) { this.jdbc=jdbc; this.users=users; }
    public record Message(long id, boolean fromAdmin, String senderName, String content, LocalDateTime createdAt) {}
    public record ThreadInfo(long id, String customerName, String customerEmail, boolean closed, LocalDateTime updatedAt, String preview, long unread, long lastMessageId) {}
    public record Messages(ThreadInfo conversation, List<Message> messages, boolean hasMore, boolean hasOlder) {}
    public record Inbox(List<ThreadInfo> conversations, boolean hasMore) {}
    private record ThreadRow(long id,long customerId,boolean closed,long lastMessageId) {}
    private static final RowMapper<Message> MESSAGE=(rs,n)->new Message(rs.getLong("id"),rs.getBoolean("from_admin"),rs.getString("sender_name"),rs.getString("content"),rs.getTimestamp("created_at").toLocalDateTime());
    private static final RowMapper<ThreadInfo> INFO=(rs,n)->new ThreadInfo(rs.getLong("id"),rs.getString("full_name"),rs.getString("email"),rs.getBoolean("closed"),rs.getTimestamp("updated_at").toLocalDateTime(),rs.getString("preview"),rs.getLong("unread"),rs.getLong("last_message_id"));
    private static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
    private static ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private AppUser actor(String email,boolean admin,boolean lock) {
        var u=(lock ? users.lockByEmail(email) : users.findByEmail(email)).orElseThrow(()->error(HttpStatus.FORBIDDEN,"Tài khoản không hợp lệ"));
        if(!u.isEnabled() || (admin ? u.getRole()!=Role.ADMIN : u.getRole()!=Role.CUSTOMER)) throw error(HttpStatus.FORBIDDEN,"Không có quyền truy cập");
        return u;
    }
    private ThreadRow thread(long id,boolean lock) {
        return jdbc.query("SELECT id,customer_id,closed,last_message_id FROM support_conversations WHERE id=?"+(lock ? " FOR UPDATE" : ""),
            (rs,n)->new ThreadRow(rs.getLong(1),rs.getLong(2),rs.getBoolean(3),rs.getLong(4)),id).stream().findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy cuộc trò chuyện"));
    }
    private Long ownId(long customerId) {
        return jdbc.query("SELECT id FROM support_conversations WHERE customer_id=?",(rs,n)->rs.getLong(1),customerId).stream().findFirst().orElse(null);
    }
    private String infoSql(boolean admin) {
        return "SELECT c.*,u.full_name,u.email,(SELECT COUNT(*) FROM support_messages m WHERE m.conversation_id=c.id AND m.from_admin="+(admin?"FALSE":"TRUE")
            +" AND m.id>c."+(admin?"admin_read_id":"customer_read_id")+") AS unread FROM support_conversations c JOIN app_users u ON u.id=c.customer_id ";
    }
    private ThreadInfo info(long id,boolean admin) { return jdbc.queryForObject(infoSql(admin)+"WHERE c.id=?",INFO,id); }
    public Inbox inbox(String email,String query,String state,int page) {
        actor(email,true,false);
        String filter=switch(state) { case "open"->" AND c.closed=FALSE";case "closed"->" AND c.closed=TRUE";
            case "unread"->" AND EXISTS(SELECT 1 FROM support_messages m WHERE m.conversation_id=c.id AND m.from_admin=FALSE AND m.id>c.admin_read_id)";default->""; };
        String q="%"+query.strip().toLowerCase(Locale.ROOT)+"%";
        var rows=jdbc.query(infoSql(true)+"WHERE (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ?)"+filter+" ORDER BY c.updated_at DESC,c.id DESC LIMIT 21 OFFSET ?",INFO,q,q,page*20L);
        return new Inbox(List.copyOf(rows.subList(0,Math.min(20,rows.size()))),rows.size()>20);
    }
    public Messages messages(String email,Long requestedId,boolean admin,long after,long before) {
        var u=actor(email,admin,false);
        Long id=admin ? requestedId : ownId(u.getId());
        if(id==null) return new Messages(null,List.of(),false,false);
        thread(id,false);
        var conversation=info(id,admin);
        String range=before>0 ? " AND id<?" : after>0 ? " AND id>?" : "";
        String order=after>0 ? " ASC" : " DESC";
        List<Object> args=new ArrayList<>(List.of(id)); if(before>0 || after>0) args.add(before>0 ? before : after);
        var rows=new ArrayList<>(jdbc.query("SELECT * FROM support_messages WHERE conversation_id=?"+range+" ORDER BY id"+order+" LIMIT 51",MESSAGE,args.toArray()));
        boolean more=rows.size()>50; if(more) rows.removeLast(); if(after==0) Collections.reverse(rows);
        boolean older=!rows.isEmpty() && Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*)>0 FROM support_messages WHERE conversation_id=? AND id<?",Boolean.class,id,rows.getFirst().id()));
        return new Messages(conversation,List.copyOf(rows),after>0 && more,older);
    }
    @Transactional
    public Message send(String email,Long requestedId,boolean admin,String text,String token) {
        var u=actor(email,admin,true); String content=text.strip();
        if(content.isBlank() || content.length()>1500 || !token.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) throw error(HttpStatus.BAD_REQUEST,"Tin nhắn không hợp lệ");
        Long id=admin ? requestedId : ownId(u.getId());
        var existing=jdbc.query("SELECT * FROM support_messages WHERE sender_id=? AND request_token=?",(rs,n)->Map.entry(rs.getLong("conversation_id"),MESSAGE.mapRow(rs,n)),u.getId(),token);
        if(!existing.isEmpty()) {
            var row=existing.getFirst();
            if(!Objects.equals(id,row.getKey()) || !content.equals(row.getValue().content())) throw error(HttpStatus.CONFLICT,"Mã gửi tin đã được dùng cho nội dung khác");
            return row.getValue();
        }
        if(jdbc.queryForObject("SELECT COUNT(*) FROM support_messages WHERE sender_id=? AND created_at>?",Integer.class,u.getId(),now().minusMinutes(1))>=20)
            throw error(HttpStatus.TOO_MANY_REQUESTS,"Bạn gửi tin hơi nhanh. Vui lòng đợi một phút.");
        if(id==null && !admin) {
            var keys=new GeneratedKeyHolder();
            jdbc.update(c->{ var ps=c.prepareStatement("INSERT INTO support_conversations(customer_id,updated_at) VALUES (?,?)",new String[]{"id"});ps.setLong(1,u.getId());ps.setObject(2,now());return ps;},keys);
            id=Objects.requireNonNull(keys.getKey()).longValue();
        }
        if(id==null) throw error(HttpStatus.NOT_FOUND,"Không tìm thấy cuộc trò chuyện");
        var conversation=thread(id,true);
        if(admin && conversation.closed()) throw error(HttpStatus.CONFLICT,"Hãy mở lại cuộc trò chuyện trước khi trả lời");
        final long conversationId=id;
        var at=now(); var keys=new GeneratedKeyHolder();
        jdbc.update(c->{var ps=c.prepareStatement("INSERT INTO support_messages(conversation_id,sender_id,sender_name,from_admin,content,request_token,created_at) VALUES (?,?,?,?,?,?,?)",new String[]{"id"});
            ps.setLong(1,conversationId);ps.setLong(2,u.getId());ps.setString(3,u.getFullName());ps.setBoolean(4,admin);ps.setString(5,content);ps.setString(6,token);ps.setObject(7,at);return ps;},keys);
        long messageId=Objects.requireNonNull(keys.getKey()).longValue();
        jdbc.update("UPDATE support_conversations SET closed=FALSE,last_message_id=?,preview=?,updated_at=? WHERE id=?",messageId,content.substring(0,Math.min(200,content.length())),at,id);
        return new Message(messageId,admin,u.getFullName(),content,at);
    }
    @Transactional
    public void read(String email,Long requestedId,boolean admin,long throughId) {
        var u=actor(email,admin,false); Long id=admin ? requestedId : ownId(u.getId());
        if(id==null || throughId<=0) return;
        var conversation=thread(id,true);
        if(throughId>conversation.lastMessageId() || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*)>0 FROM support_messages WHERE conversation_id=? AND id=?",Boolean.class,id,throughId)))
            throw error(HttpStatus.BAD_REQUEST,"Mốc đọc không hợp lệ");
        String field=admin?"admin_read_id":"customer_read_id";
        jdbc.update("UPDATE support_conversations SET "+field+"=? WHERE id=? AND "+field+"<?",throughId,id,throughId);
    }
    @Transactional
    public void close(String email,long id,boolean closed) {
        actor(email,true,false);thread(id,true);
        jdbc.update("UPDATE support_conversations SET closed=? WHERE id=?",closed,id);
    }
}
