package vn.sunicoffee.service;

import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.config.GooglePrincipal;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.UserRepository;

@Service
public class GoogleAccountService {
    public static final String PENDING="googleLinkPending";
    public record Pending(String subject,String email,Instant expires) implements java.io.Serializable {}
    private final JdbcTemplate db;
    private final UserRepository users;
    private final UserService registration;
    private final PasswordEncoder encoder;
    public GoogleAccountService(JdbcTemplate db,UserRepository users,UserService registration,PasswordEncoder encoder) {
        this.db=db;this.users=users;this.registration=registration;this.encoder=encoder;
    }
    @Transactional
    public GooglePrincipal resolve(OidcUser identity,HttpSession session) {
        session.removeAttribute(PENDING);
        // Called only after Spring has validated the code, signature, issuer, audience and nonce.
        String issuer=String.valueOf(identity.getIdToken().getClaims().get("iss"));
        String subject=identity.getSubject();
        String email=identity.getEmail();
        if(!Set.of("https://accounts.google.com","accounts.google.com").contains(issuer)
            || !Boolean.TRUE.equals(identity.getEmailVerified()) || subject==null || subject.isBlank() || subject.length()>255
            || email==null || email.isBlank() || email.length()>190) throw denied("invalid_google_identity");
        email=email.strip().toLowerCase(Locale.ROOT);
        var ids=db.queryForList("SELECT user_id FROM google_accounts WHERE subject=?",Long.class,subject);
        AppUser account;
        if(!ids.isEmpty()) account=users.findById(ids.getFirst()).orElseThrow(()->denied("account_unavailable"));
        else {
            var existing=users.findByEmail(email);
            if(existing.isPresent()) {
                if(!existing.get().isEnabled()) throw denied("account_unavailable");
                session.setAttribute(PENDING,new Pending(subject,email,Instant.now().plusSeconds(300)));
                throw denied("link_required");
            }
            String name=identity.getFullName();
            if(name==null || name.isBlank()) name="Khách hàng";
            account=registration.create(name.substring(0,Math.min(name.length(),100)),email,UUID.randomUUID().toString(),Role.CUSTOMER);
            users.flush();
            db.update("INSERT INTO google_accounts(subject,user_id) VALUES (?,?)",subject,account.getId());
        }
        if(!account.isEnabled()) throw denied("account_unavailable");
        return new GooglePrincipal(account,identity);
    }
    @Transactional
    public void link(Pending pending,String password) {
        if(pending==null || !pending.expires().isAfter(Instant.now())) throw new BusinessException("Yêu cầu đã hết hạn. Hãy chọn đăng nhập Google lại.");
        var account=users.lockByEmail(pending.email()).orElseThrow(()->new BusinessException("Không thể liên kết tài khoản."));
        if(!account.isEnabled() || password==null || password.length()>100 || !encoder.matches(password,account.getPasswordHash()))
            throw new BusinessException("Mật khẩu chưa đúng hoặc tài khoản không khả dụng.");
        if(!db.queryForList("SELECT subject FROM google_accounts WHERE user_id=? OR subject=?",String.class,account.getId(),pending.subject()).isEmpty())
            throw new BusinessException("Tài khoản đã có liên kết Google. Hãy đăng nhập bằng tài khoản Google đã liên kết.");
        db.update("INSERT INTO google_accounts(subject,user_id) VALUES (?,?)",pending.subject(),account.getId());
    }
    private OAuth2AuthenticationException denied(String code) { return new OAuth2AuthenticationException(new OAuth2Error(code)); }
}
