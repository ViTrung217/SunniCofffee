package vn.sunicoffee;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.Role;
import vn.sunicoffee.service.*;
import vn.sunicoffee.repository.UserRepository;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"app.google.enabled=true","app.google.client-id=test.apps.googleusercontent.com","app.google.client-secret=test-secret"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class GoogleLoginIntegrationTest {
    @Autowired GoogleAccountService google;
    @Autowired UserService registration;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    private OidcUser identity(String subject,String email,boolean verified) {
        return new DefaultOidcUser(java.util.List.of(),new OidcIdToken("test-token",Instant.now(),Instant.now().plusSeconds(600),Map.of("iss","https://accounts.google.com","sub",subject,"email",email,"email_verified",verified,"name","Nguyễn An")));
    }
    @Test void createsCustomerAndReusesStableGoogleSubject() {
        var first=google.resolve(identity("google-new","google.new@example.com",true),new MockHttpSession());
        assertThat(first.getName()).isEqualTo("google.new@example.com");
        assertThat(first.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
        var again=google.resolve(identity("google-new","changed@example.com",true),new MockHttpSession());
        assertThat(again.getName()).isEqualTo(first.getName());
        assertThat(users.existsByEmail("changed@example.com")).isFalse();
    }
    @Test void refusesUnverifiedIdentity() {
        assertThatThrownBy(()->google.resolve(identity("unverified","unverified@example.com",false),new MockHttpSession())).isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(users.existsByEmail("unverified@example.com")).isFalse();
    }
    @Test void existingEmailRequiresPasswordAndKeepsAccount() {
        var existing=registration.create("Khách hiện tại","existing.google@example.com","Existing!2026",Role.CUSTOMER);
        users.flush();
        var session=new MockHttpSession();
        assertThatThrownBy(()->google.resolve(identity("google-existing",existing.getEmail(),true),session)).isInstanceOf(OAuth2AuthenticationException.class);
        var pending=(GoogleAccountService.Pending)session.getAttribute(GoogleAccountService.PENDING);
        assertThatThrownBy(()->google.link(pending,"incorrect")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->google.link(new GoogleAccountService.Pending(pending.subject(),pending.email(),Instant.now().minusSeconds(1)),"Existing!2026")).isInstanceOf(BusinessException.class);
        google.link(pending,"Existing!2026");
        var principal=google.resolve(identity("google-existing",existing.getEmail(),true),session);
        assertThat(principal.getName()).isEqualTo(existing.getEmail());
        assertThat(users.findByEmail(existing.getEmail()).orElseThrow().getId()).isEqualTo(existing.getId());
        assertThat(session.getAttribute(GoogleAccountService.PENDING)).isNull();
    }
    @Test void disabledAccountAndRevokedSessionCannotAccessPrivatePages() throws Exception {
        var principal=google.resolve(identity("google-state","google.state@example.com",true),new MockHttpSession());
        var account=users.findByEmail(principal.getName()).orElseThrow();
        var auth=new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(principal,principal.getAuthorities(),"google");
        mvc.perform(get("/admin").with(authentication(auth))).andExpect(status().isForbidden());
        account.setAuthVersion(account.getAuthVersion()+1);users.saveAndFlush(account);
        mvc.perform(get("/favorites").with(authentication(auth))).andExpect(redirectedUrl("/login?expired"));
        account.setEnabled(false);users.saveAndFlush(account);
        assertThatThrownBy(()->google.resolve(identity("google-state",account.getEmail(),true),new MockHttpSession())).isInstanceOf(OAuth2AuthenticationException.class);
    }
    @Test void loginRendersAndAuthorizationUsesStateAndNonce() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Tiếp tục với Google")));
        var pendingSession=new MockHttpSession();
        pendingSession.setAttribute(GoogleAccountService.PENDING,new GoogleAccountService.Pending("subject","link@example.com",Instant.now().plusSeconds(300)));
        mvc.perform(get("/login").param("googleError","").session(pendingSession)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Liên kết với Google")));
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location",org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.startsWith("https://accounts.google.com/"),org.hamcrest.Matchers.containsString("state="),org.hamcrest.Matchers.containsString("nonce="))));
        mvc.perform(get("/login/oauth2/code/google").param("code","untrusted").param("state","unmatched")).andExpect(redirectedUrl("/login?googleError"));
        mvc.perform(post("/login/google/link").param("password","whatever")).andExpect(status().isForbidden());
        mvc.perform(post("/login/google/link").with(csrf()).param("password","whatever")).andExpect(redirectedUrl("/login?googleError"));
    }
}
