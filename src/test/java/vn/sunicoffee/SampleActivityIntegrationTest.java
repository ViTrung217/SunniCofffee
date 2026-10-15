package vn.sunicoffee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.service.SampleActivityService;
import vn.sunicoffee.service.SampleCatalogService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SampleActivityIntegrationTest {
    @Autowired SampleActivityService activity;
    @Autowired SampleCatalogService catalog;
    @Autowired JdbcTemplate db;
    @Test void importsConsistentHistoryOnceWithoutChangingExistingStock() throws Exception {
        catalog.refresh();
        var stocks=db.queryForList("SELECT id,stock,price FROM products ORDER BY id");
        int previous=db.queryForObject("SELECT COUNT(*) FROM customer_orders",Integer.class);
        assertThat(activity.populate()).isTrue();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM app_users WHERE email LIKE '%@history.sunicoffee.example'",Integer.class)).isEqualTo(24);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM customer_orders",Integer.class)).isEqualTo(previous+72);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_reviews r JOIN app_users u ON u.id=r.user_id WHERE u.email LIKE '%@history.sunicoffee.example'",Integer.class)).isEqualTo(60);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_favorites f JOIN app_users u ON u.id=f.user_id WHERE u.email LIKE '%@history.sunicoffee.example'",Integer.class)).isEqualTo(120);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_reviews r JOIN app_users u ON u.id=r.user_id WHERE u.email LIKE '%@history.sunicoffee.example' AND NOT EXISTS (SELECT 1 FROM customer_orders o JOIN order_items i ON i.order_id=o.id WHERE o.user_id=r.user_id AND i.product_id=r.product_id AND o.status='COMPLETED')",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM customer_orders o JOIN app_users u ON u.id=o.user_id WHERE u.email LIKE '%@history.sunicoffee.example' AND (o.total <> o.shipping_fee + (SELECT SUM(i.unit_price*i.quantity) FROM order_items i WHERE i.order_id=o.id) OR (o.status='COMPLETED' AND (o.payment_status<>'PAID' OR o.received_amount<>o.total)))",Integer.class)).isZero();
        assertThat(activity.populate()).isFalse();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM customer_orders",Integer.class)).isEqualTo(previous+72);
        assertThat(db.queryForList("SELECT id,stock,price FROM products ORDER BY id")).isEqualTo(stocks);
    }
}
