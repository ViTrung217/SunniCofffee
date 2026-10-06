package vn.sunicoffee.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.sunicoffee.repository.UserRepository;

/** Re-check persistent account state so old sessions cannot retain revoked access. */
public class AccountStateFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public AccountStateFilter(UserRepository users) { this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var account=users.findByEmail(auth.getName());
            boolean invalid=account.isEmpty() || !account.get().isEnabled();
            if(!invalid && auth.getPrincipal() instanceof AccountPrincipal principal)
                invalid=principal.getAuthVersion()!=account.get().getAuthVersion();
            if(invalid) {
                new SecurityContextLogoutHandler().logout(request,response,auth);
                response.sendRedirect(request.getContextPath()+"/login?expired");
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
