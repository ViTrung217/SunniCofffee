package vn.sunicoffee;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.CheckoutForm;
import vn.sunicoffee.payment.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"app.sepay.enabled=true","app.sepay.sandbox=true","app.sepay.api-token=test-only-not-real",
    "app.sepay.bank-account-id=11111111-1111-1111-1111-111111111111","app.sepay.account-number=0123456789",
    "app.sepay.qr-account=0123456789","app.sepay.bank=Vietcombank","app.sepay.holder=TEST ACCOUNT"})
@ActiveProfiles("test")
class SepayPaymentConcurrencyTest {
    @Autowired UserService users; @Autowired UserRepository accounts; @Autowired CategoryRepository categories;
    @Autowired ProductRepository products; @Autowired CartService carts; @Autowired OrderService orders;
    @Autowired PaymentStore payments; @Autowired JdbcTemplate jdbc;
    @Test void simultaneousReconciliationCreditsOnlyOnce() throws Exception {
        String suffix=UUID.randomUUID().toString(),email="pay-race-"+suffix+"@example.test";
        users.create("Khách kiểm thử",email,"TestPass!2026",Role.CUSTOMER);long userId=accounts.findByEmail(email).orElseThrow().getId();
        var c=new Category();c.setCode("QR"+suffix.substring(0,8));c.setName("Mẫu");c.setDescription("Mẫu");c=categories.save(c);
        var p=new Product();p.setSku("QR"+suffix.substring(0,8));p.setName("Mẫu");p.setDescription("Mẫu");p.setCategory(c);p.setPrice(new BigDecimal("50000"));p.setStock(2);p.setSalesUnit("túi");p.setActive(true);p=products.save(p);
        Long orderId=null;
        try {
            carts.add(email,p.getId(),1);var f=new CheckoutForm();f.setCheckoutToken(suffix);f.setRecipientName("Khách");f.setAddress("Địa chỉ mẫu kiểm thử");f.setPhone("0900000000");f.setPaymentMethod(PaymentMethod.SEPAY);
            var o=orders.checkout(email,f);orderId=o.getId();
            assertThat(o.getCreatedAt().getNano()).isZero();
            LocalDateTime storedCreation=jdbc.queryForObject("SELECT created_at FROM customer_orders WHERE id=?",
                (rs,n)->rs.getObject(1,LocalDateTime.class),orderId);
            assertThat(storedCreation).isEqualTo(o.getCreatedAt());
            var t=new SepayClient.Transfer(UUID.randomUUID().toString(),o.getSepayBankAccountId(),o.getSepayAccountNumber(),"","in",o.getTotal(),BigDecimal.ZERO,o.getPaymentCode(),o.getCreatedAt().plusNanos(900_000_000),"TEST-RACE");
            try(var pool=Executors.newFixedThreadPool(2)) {
                var start=new CountDownLatch(1);Callable<Void> task=()->{start.await();payments.apply(o.getId(),List.of(t));return null;};
                var a=pool.submit(task);var b=pool.submit(task);start.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
            }
            assertThat(payments.view(o.getId(),email,false).received()).isEqualByComparingTo(o.getTotal());
            assertThat(payments.view(o.getId(),email,false).receipts()).hasSize(1);
        } finally {
            if(orderId!=null) for(String table:List.of("payment_receipts","order_events","stock_movements","order_items")) jdbc.update("DELETE FROM "+table+" WHERE order_id=?",orderId);
            if(orderId!=null) jdbc.update("DELETE FROM customer_orders WHERE id=?",orderId);
            jdbc.update("DELETE FROM cart_items WHERE user_id=?",userId);jdbc.update("DELETE FROM stock_movements WHERE product_id=?",p.getId());
            products.deleteById(p.getId());categories.deleteById(c.getId());accounts.deleteById(userId);
        }
    }
}
