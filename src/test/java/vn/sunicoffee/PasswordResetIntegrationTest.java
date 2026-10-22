package vn.sunicoffee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import vn.sunicoffee.service.*;
import vn.sunicoffee.domain.Role;
import vn.sunicoffee.config.AccountPrincipal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.password-reset.enabled=true","app.password-reset.from=shop@example.com"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class PasswordResetIntegrationTest {
    @Autowired PasswordResetService resets;
    @Autowired UserService users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    private String token() {
        var captured=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail,atLeastOnce()).send(captured.capture());
        return captured.getValue().getText().split("token=")[1].split("\\s")[0];
    }
    @Test void singleUseHashedTokenResetsPasswordAndRevokesSessions() {
        var u=users.create("Khách","reset@example.com","Password!123",Role.CUSTOMER);
        resets.request(u.getEmail());String token=token();
        assertThat(db.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id=?",String.class,u.getId())).isNotEqualTo(token);
        long version=u.getAuthVersion();resets.reset(token,"Updated!123","Updated!123");
        assertThat(encoder.matches("Updated!123",u.getPasswordHash())).isTrue();assertThat(u.getAuthVersion()).isEqualTo(version+1);
        assertThatThrownBy(()->resets.reset(token,"Again!123","Again!123")).isInstanceOf(BusinessException.class);
    }
    @Test void expiryDisabledUsersAndInvalidTokensCannotReset() {
        var u=users.create("Khách","expiry@example.com","Password!123",Role.CUSTOMER);
        resets.request(u.getEmail());String token=token();
        db.update("UPDATE password_reset_tokens SET expires_at=? WHERE user_id=?",java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1),u.getId());
        assertThatThrownBy(()->resets.reset(token,"Updated!123","Updated!123")).isInstanceOf(BusinessException.class);
        db.update("UPDATE password_reset_tokens SET expires_at=? WHERE user_id=?",java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(1),u.getId());
        u.setEnabled(false);
        assertThatThrownBy(()->resets.reset(token,"Updated!123","Updated!123")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->resets.reset("invalid","Updated!123","Updated!123")).isInstanceOf(BusinessException.class);
        assertThat(encoder.matches("Password!123",u.getPasswordHash())).isTrue();
    }
    @Test void throttlesPerAccountAndNewRequestInvalidatesOldLink() {
        var u=users.create("Khách","retry@example.com","Password!123",Role.CUSTOMER);
        resets.request(u.getEmail());String old=token();resets.request(u.getEmail());verify(mail,times(1)).send(any(SimpleMailMessage.class));
        db.update("UPDATE password_reset_tokens SET created_at=? WHERE user_id=?",java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(2),u.getId());
        resets.request(u.getEmail());String fresh=token();assertThat(fresh).isNotEqualTo(old);
        assertThatThrownBy(()->resets.reset(old,"Updated!123","Updated!123")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->resets.reset(fresh,"Updated!123","different")).isInstanceOf(BusinessException.class);
        resets.reset(fresh,"Updated!123","Updated!123");
    }
    @Test void publicFormsProtectCsrfAndDoNotRevealUnknownEmail() throws Exception {
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mvc.perform(get("/reset-password").param("token","abc")).andExpect(status().isOk()).andExpect(header().string("Referrer-Policy","no-referrer"));
        mvc.perform(post("/forgot-password").param("email","unknown@example.com")).andExpect(status().isForbidden());
        mvc.perform(post("/reset-password").param("token","abc").param("password","Updated!123").param("confirmPassword","Updated!123")).andExpect(status().isForbidden());
        mvc.perform(post("/forgot-password").with(csrf()).param("email","unknown@example.com")).andExpect(status().isOk()).andExpect(model().attribute("sent",true));
        verifyNoInteractions(mail);
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentUseSucceedsOnlyOnce() throws Exception {
        var u=users.create("Khách","concurrent.reset@example.com","Password!123",Role.CUSTOMER);
        try {
            resets.request(u.getEmail());String code=token();
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> attempt=()->{start.await();try {resets.reset(code,"Updated!123","Updated!123");return true;}catch(BusinessException ex){return false;}};
            try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var a=pool.submit(attempt);var b=pool.submit(attempt);start.countDown();
                assertThat(java.util.List.of(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            }
        } finally {db.update("DELETE FROM app_users WHERE id=?",u.getId());}
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failedMailDoesNotLeaveUsableToken() {
        var u=users.create("Khách","failed.mail@example.com","Password!123",Role.CUSTOMER);
        try {
            doThrow(new MailSendException("Test delivery failure")).when(mail).send(any(SimpleMailMessage.class));
            assertThatThrownBy(()->resets.request(u.getEmail())).isInstanceOf(MailSendException.class);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,u.getId())).isZero();
        } finally {db.update("DELETE FROM app_users WHERE id=?",u.getId());}
    }
    @Test void profileSupportLinkRespectsRole() throws Exception {
        var admin=users.create("Quản trị","link.admin@example.com","Password!123",Role.ADMIN);
        var customer=users.create("Khách","link.customer@example.com","Password!123",Role.CUSTOMER);
        mvc.perform(get("/account").with(user(new AccountPrincipal(admin)))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/support\">Chat với cửa hàng")));
        mvc.perform(get("/account").with(user(new AccountPrincipal(customer)))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/support\">Chat với cửa hàng")));
    }
}
