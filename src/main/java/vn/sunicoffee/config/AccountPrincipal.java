package vn.sunicoffee.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import vn.sunicoffee.domain.AppUser;
import java.util.List;

public class AccountPrincipal extends User {
    private static final long serialVersionUID=1L;
    private final long authVersion;
    public AccountPrincipal(AppUser user) {
        super(user.getEmail(),user.getPasswordHash(),user.isEnabled(),true,true,true,
            List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())));
        authVersion=user.getAuthVersion();
    }
    public long getAuthVersion() { return authVersion; }
}
