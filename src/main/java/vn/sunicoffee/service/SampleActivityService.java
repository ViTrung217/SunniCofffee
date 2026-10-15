package vn.sunicoffee.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fictional local history. A locked marker makes the entire import atomic and repeatable. */
@Service
public class SampleActivityService {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    public SampleActivityService(JdbcTemplate db, PasswordEncoder passwords) { this.db=db; this.passwords=passwords; }
    private static final String[] NAMES={"Nguyễn Minh Anh","Trần Hoàng Nam","Lê Thu Hà","Phạm Quang Huy","Hoàng Ngọc Linh","Vũ Đức Anh","Đặng Phương Thảo","Bùi Gia Bảo","Đỗ Khánh Vy","Hồ Minh Khang","Ngô Hải Yến","Dương Tuấn Kiệt","Lý Thanh Tâm","Mai Nhật Minh","Võ Bảo Ngọc","Phan Đức Duy","Đinh Thùy Dương","Cao Anh Thư","Tạ Thành Đạt","Trịnh Kim Chi","Lương Quốc Bảo","Đào Hà My","Huỳnh Minh Châu","Tô Việt Hoàng"};
    private static final String[] COMMENTS={"Đóng gói cẩn thận, nhận đủ hàng. Mình sẽ quay lại khi cần mua thêm.","Sản phẩm đúng mô tả, phù hợp với nhu cầu dùng tại nhà.","Đã dùng vài tuần, nhìn chung hài lòng với lựa chọn này.","Tư vấn dễ hiểu, đơn được chuẩn bị khá nhanh.","Hàng ổn nhưng hộp ngoài hơi móp khi nhận, mong lần sau đóng gói kỹ hơn.","Mua thêm lần nữa vì lần trước dùng thấy phù hợp.","Giá phù hợp với ngân sách của mình, nhận hàng đầy đủ.","Giao hơi lâu hơn mong đợi, bù lại sản phẩm được bọc kỹ.","Mình mua làm quà, người nhận khá hài lòng.","Lần đầu đặt ở cửa hàng, trải nghiệm nhìn chung tốt."};
    private record Item(long id,String name,String sku,BigDecimal price,int stock) {}
    @Transactional
    public boolean populate() {
        var done=db.queryForObject("SELECT completed_at FROM activity_seed_runs WHERE seed_key='store-history-v1' FOR UPDATE",(r,n)->r.getObject(1));
        if(done!=null) return false;
        var products=db.query("SELECT id,name,sku,price,stock FROM products WHERE active=TRUE AND sku LIKE 'DEMO-%' ORDER BY sku",(r,n)->new Item(r.getLong(1),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getInt(5)));
        if(products.size()<12) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Chưa tạo lịch sử: cần ít nhất 12 sản phẩm danh mục đang bán.");
            return false;
        }
        var now=LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0);
        long[] users=new long[NAMES.length];
        String hash=passwords.encode(UUID.randomUUID().toString());
        for(int u=0;u<NAMES.length;u++) {
            String email="customer."+(u+1)+"@history.sunicoffee.example";
            // Never adopt an existing account as a fictional customer.
            db.update("INSERT INTO app_users(full_name,email,password_hash,role,enabled,phone,address) VALUES (?,?,?,'CUSTOMER',TRUE,?,?)",NAMES[u],email,hash,"0000000000",address(u));
            users[u]=db.queryForObject("SELECT id FROM app_users WHERE email=?",Long.class,email);
            for(int f=0;f<5;f++) db.update("INSERT INTO product_favorites(user_id,product_id,created_at) VALUES (?,?,?)",users[u],products.get((u*7+f)%products.size()).id(),now.minusDays(2+u+f));
        }
        for(int i=0;i<72;i++) {
            int u=i%24; var item=products.get((u*7+(i/24)*5)%products.size()); int qty=1+i%3;
            boolean cancelled=i%6==0;
            var created=now.minusDays(120-i*115/72).withHour(8+i%12).withMinute(i*7%60);
            var finished=created.plusDays(3); var total=item.price().multiply(BigDecimal.valueOf(qty)).add(BigDecimal.valueOf(25000));
            String token=UUID.nameUUIDFromBytes(("store-history-v1-order-"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            db.update("INSERT INTO customer_orders(user_id,checkout_token,recipient_name,phone,address,note,status,total,created_at,shipping_fee,payment_method,payment_status,received_amount,completed_at,paid_at,cod_settled_at,shipped_at,carrier,tracking_code) VALUES (?,?,?,?,?, '',?,?,?,25000,'COD',?,?,?,?,?,?,?,?)",users[u],token,NAMES[u],"0000000000",address(u),cancelled?"CANCELLED":"COMPLETED",total,created,cancelled?"UNPAID":"PAID",cancelled?BigDecimal.ZERO:total,cancelled?null:finished,cancelled?null:finished,cancelled?null:finished,cancelled?null:created.plusDays(1),cancelled?"":"Giao hàng tiêu chuẩn",cancelled?"":"SC"+String.format("%08d",41000+i));
            long order=db.queryForObject("SELECT id FROM customer_orders WHERE checkout_token=?",Long.class,token);
            db.update("INSERT INTO order_items(order_id,product_id,product_name,sku,unit_price,quantity) VALUES (?,?,?,?,?,?)",order,item.id(),item.name(),item.sku(),item.price(),qty);
            event(order,"PENDING",created,"Đã tiếp nhận đơn hàng");
            event(order,"CONFIRMED",created.plusHours(2),"Đã xác nhận đơn hàng");
            if(cancelled) event(order,"CANCELLED",created.plusHours(4),"Khách thay đổi nhu cầu");
            else {
                event(order,"SHIPPING",created.plusDays(1),"Đã bàn giao vận chuyển");
                event(order,"COMPLETED",finished,"Đã giao hàng và thu COD");
                // Paired historical receipt/sale preserves current stock and a consistent ledger.
                movement(item,order,"RECEIPT",qty,item.stock()+qty,created.minusHours(1));
                movement(item,order,"SALE",-qty,item.stock(),created);
                int rating=(i%10==4 || i%10==7)?3:(i%3==0?4:5);
                db.update("INSERT INTO product_reviews(user_id,product_id,rating,comment,updated_at) VALUES (?,?,?,?,?)",users[u],item.id(),rating,COMMENTS[i%COMMENTS.length],finished.plusDays(1));
            }
        }
        db.update("UPDATE activity_seed_runs SET completed_at=? WHERE seed_key='store-history-v1'",now);
        return true;
    }
    private String address(int u) { return "Căn "+(101+u)+", khu An Nhiên, "+new String[]{"Hà Nội","Đà Nẵng","TP. Hồ Chí Minh","Cần Thơ"}[u%4]; }
    private void event(long order,String status,LocalDateTime at,String note) { db.update("INSERT INTO order_events(order_id,status,actor,note,created_at) VALUES (?,?, 'Hệ thống',?,?)",order,status,note,at); }
    private void movement(Item item,long order,String type,int delta,int balance,LocalDateTime at) { db.update("INSERT INTO stock_movements(product_id,order_id,type,quantity_delta,balance_after,reason,created_at) VALUES (?,?,?,?,?,'Lịch sử tổng hợp store-history-v1',?)",item.id(),order,type,delta,balance,at); }
}
