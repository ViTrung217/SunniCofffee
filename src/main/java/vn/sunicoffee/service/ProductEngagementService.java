package vn.sunicoffee.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.domain.Product;
import vn.sunicoffee.repository.*;

@Service
@Transactional(readOnly=true)
public class ProductEngagementService {
    private final JdbcTemplate jdbc; private final UserRepository users; private final ProductRepository products;
    public ProductEngagementService(JdbcTemplate j,UserRepository u,ProductRepository p) {jdbc=j;users=u;products=p;}
    public record Review(long userId,String author,int rating,String comment,LocalDateTime updatedAt) {}
    public record Summary(long count,double average) {}
    private long user(String email,boolean lock) {
        return (lock?users.lockByEmail(email):users.findByEmail(email)).filter(u->u.isEnabled())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN)).getId();
    }
    private void active(long id) {products.findById(id).filter(Product::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    public boolean favorite(String email,long id) {return email!=null && jdbc.queryForObject("SELECT COUNT(*) FROM product_favorites WHERE user_id=? AND product_id=?",Integer.class,user(email,false),id)>0;}
    @Transactional public void favorite(String email,long id,boolean selected) {
        long uid=user(email,true);
        if(selected) {
            active(id);
            if(!favorite(email,id)) jdbc.update("INSERT INTO product_favorites(user_id,product_id,created_at) VALUES (?,?,?)",uid,id,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        } else jdbc.update("DELETE FROM product_favorites WHERE user_id=? AND product_id=?",uid,id);
    }
    public Page<Product> favorites(String email,int page) {return products.favorites(user(email,false),PageRequest.of(Math.max(0,page),12));}
    public boolean eligible(String email,long id) {
        if(email==null) return false;
        return jdbc.queryForObject("SELECT COUNT(*) FROM order_items i JOIN customer_orders o ON o.id=i.order_id WHERE o.user_id=? AND i.product_id=? AND o.status='COMPLETED'",Integer.class,user(email,false),id)>0;
    }
    public List<Review> reviews(long id,int page) {
        return jdbc.query("SELECT r.user_id,u.full_name,r.rating,r.comment,r.updated_at FROM product_reviews r JOIN app_users u ON u.id=r.user_id WHERE r.product_id=? ORDER BY r.updated_at DESC,r.user_id LIMIT 10 OFFSET ?",
            (r,n)->new Review(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getObject(5,LocalDateTime.class)),id,Math.max(0,page)*10L);
    }
    public Summary summary(long id) {return jdbc.queryForObject("SELECT COUNT(*),COALESCE(AVG(rating),0) FROM product_reviews WHERE product_id=?",(r,n)->new Summary(r.getLong(1),r.getDouble(2)),id);}
    public Review own(String email,long id) {
        if(email==null)return null;
        return jdbc.query("SELECT r.user_id,u.full_name,r.rating,r.comment,r.updated_at FROM product_reviews r JOIN app_users u ON u.id=r.user_id WHERE r.product_id=? AND r.user_id=?",
            (r,n)->new Review(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getObject(5,LocalDateTime.class)),id,user(email,false)).stream().findFirst().orElse(null);
    }
    @Transactional public void review(String email,long id,int rating,String comment) {
        long uid=user(email,true);active(id);
        if(!eligible(email,id))throw new BusinessException("Bạn cần mua và nhận sản phẩm trước khi đánh giá.");
        if(rating<1 || rating>5 || comment==null || comment.isBlank() || comment.length()>1500)throw new BusinessException("Chọn 1–5 sao và viết nhận xét từ 1 đến 1500 ký tự.");
        var now=LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int updated=jdbc.update("UPDATE product_reviews SET rating=?,comment=?,updated_at=? WHERE user_id=? AND product_id=?",rating,comment.strip(),now,uid,id);
        if(updated==0)jdbc.update("INSERT INTO product_reviews(user_id,product_id,rating,comment,updated_at) VALUES (?,?,?,?,?)",uid,id,rating,comment.strip(),now);
    }
    @Transactional public void deleteReview(String email,long id) {jdbc.update("DELETE FROM product_reviews WHERE user_id=? AND product_id=?",user(email,true),id);}
}
