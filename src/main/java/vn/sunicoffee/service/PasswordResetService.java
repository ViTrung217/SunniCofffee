package vn.sunicoffee.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.repository.UserRepository;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mail;
    private final boolean enabled;
    private final String baseUrl,from;
    public PasswordResetService(UserRepository users,JdbcTemplate db,PasswordEncoder encoder,ObjectProvider<JavaMailSender> mail,
            @Value("${app.password-reset.enabled:false}") boolean enabled,
            @Value("${app.password-reset.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.password-reset.from:}") String from) {
        this.users=users;this.db=db;this.encoder=encoder;this.mail=mail;this.enabled=enabled;
        this.baseUrl=baseUrl.replaceAll("/+$","");this.from=from;
        if(enabled && (!this.baseUrl.matches("https?://[^\\s?#]+") || from.isBlank()))
            throw new IllegalArgumentException("Configure password reset base URL and sender");
    }
    public boolean available() { return enabled && mail.getIfAvailable()!=null; }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public void request(String email) {
        if(!available() || email==null || email.length()>190) return;
        var found=users.lockByEmail(email.strip().toLowerCase(Locale.ROOT));
        if(found.isEmpty() || !found.get().isEnabled()) return;
        var user=found.get();var now=now();
        var created=db.query("SELECT created_at FROM password_reset_tokens WHERE user_id=?",(r,n)->r.getObject(1,LocalDateTime.class),user.getId());
        if(!created.isEmpty() && created.getFirst().isAfter(now.minusMinutes(1))) return;
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=HexFormat.of().formatHex(bytes);
        db.update("DELETE FROM password_reset_tokens WHERE user_id=?",user.getId());
        db.update("INSERT INTO password_reset_tokens(user_id,token_hash,created_at,expires_at) VALUES (?,?,?,?)",user.getId(),hash(token),now,now.plusMinutes(15));
        var message=new SimpleMailMessage();message.setFrom(from);message.setTo(user.getEmail());
        message.setSubject("SuniCoffee — Đặt lại mật khẩu");
        message.setText("Mở liên kết sau để đặt lại mật khẩu (có hiệu lực 15 phút, chỉ dùng một lần):\n"+baseUrl+"/reset-password?token="+token+"\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
        // Failure rolls back the new token. Never log the message or credentials.
        mail.getObject().send(message);
    }
    @Transactional
    public void reset(String token,String password,String confirm) {
        if(!available() || token==null || !token.matches("[a-f0-9]{64}")) throw invalid();
        if(password==null || !password.equals(confirm) || password.length()<8 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessException("Mật khẩu cần 8–64 ký tự, tối đa 72 byte UTF-8 và hai lần nhập phải khớp.");
        String digest=hash(token);
        var ids=db.queryForList("SELECT user_id FROM password_reset_tokens WHERE token_hash=?",Long.class,digest);
        if(ids.isEmpty()) throw invalid();
        var user=users.lockById(ids.getFirst()).orElseThrow(this::invalid);
        // Recheck after locking the user, so concurrent use or a newer request cannot reuse the token.
        var expiry=db.query("SELECT expires_at FROM password_reset_tokens WHERE user_id=? AND token_hash=? FOR UPDATE",(r,n)->r.getObject(1,LocalDateTime.class),user.getId(),digest);
        if(!user.isEnabled() || expiry.isEmpty() || !expiry.getFirst().isAfter(now())) throw invalid();
        user.setPasswordHash(encoder.encode(password));user.setAuthVersion(user.getAuthVersion()+1);
        db.update("DELETE FROM password_reset_tokens WHERE user_id=?",user.getId());
    }
    private BusinessException invalid() { return new BusinessException("Liên kết không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu liên kết mới."); }
}
