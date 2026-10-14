package vn.sunicoffee;

import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.sunicoffee.domain.Role;
import vn.sunicoffee.repository.UserRepository;
import vn.sunicoffee.service.UserService;
import vn.sunicoffee.support.SupportService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SupportConcurrencyTest {
    @Autowired UserService users; @Autowired UserRepository accounts; @Autowired SupportService support; @Autowired JdbcTemplate jdbc;
    @Test void simultaneousFirstSendWithSameTokenCreatesOneThreadAndMessage() throws Exception {
        String email="support-race-"+UUID.randomUUID()+"@example.test", token=UUID.randomUUID().toString();
        users.create("Khách kiểm thử đồng thời",email,"TestPass!2026",Role.CUSTOMER);
        long id=accounts.findByEmail(email).orElseThrow().getId();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<SupportService.Message> task=()-> {start.await();return support.send(email,null,false,"Chỉ một tin",token);};
            var a=pool.submit(task);var b=pool.submit(task);start.countDown();
            assertThat(a.get(15,TimeUnit.SECONDS).id()).isEqualTo(b.get(15,TimeUnit.SECONDS).id());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM support_conversations WHERE customer_id=?",Long.class,id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM support_messages WHERE sender_id=?",Long.class,id)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM support_messages WHERE sender_id=?",id);
            jdbc.update("DELETE FROM support_conversations WHERE customer_id=?",id);
            accounts.deleteById(id);
        }
    }
}
