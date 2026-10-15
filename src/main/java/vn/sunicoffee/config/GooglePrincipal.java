package vn.sunicoffee.config;

import java.util.Map;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import vn.sunicoffee.domain.AppUser;

/** Keeps the application's email name and authVersion checks for OIDC sessions. */
public class GooglePrincipal extends AccountPrincipal implements OidcUser {
    private final OidcUser google;
    public GooglePrincipal(AppUser user,OidcUser google) { super(user); this.google=google; }
    @Override public String getName() { return getUsername(); }
    @Override public Map<String,Object> getAttributes() { return google.getAttributes(); }
    @Override public Map<String,Object> getClaims() { return google.getClaims(); }
    @Override public OidcUserInfo getUserInfo() { return google.getUserInfo(); }
    @Override public OidcIdToken getIdToken() { return google.getIdToken(); }
}
