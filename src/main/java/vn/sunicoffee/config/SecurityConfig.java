package vn.sunicoffee.config;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.sunicoffee.repository.UserRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .map(u -> (UserDetails)new AccountPrincipal(u))
            .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,UserRepository users,org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> clients,vn.sunicoffee.service.GoogleAccountService google,@org.springframework.beans.factory.annotation.Value("${app.security.login-attempts-per-minute:20}") int loginLimit) throws Exception {
        if(clients.getIfAvailable()!=null) {
            var delegate=new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService();
            http.oauth2Login(oauth -> oauth.loginPage("/login")
                .userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var identity=delegate.loadUser(request);
                    var servlet=(org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                    try { return google.resolve(identity,servlet.getRequest().getSession()); }
                    catch(org.springframework.dao.DataIntegrityViolationException ex) {
                        throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("account_conflict"));
                    }
                }))
                .defaultSuccessUrl("/",false).failureUrl("/login?googleError"));
        }
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,"/products/*/favorite","/products/*/reviews","/products/*/reviews/delete").authenticated()
                .requestMatchers("/chat", "/chat/messages", "/chat/sources/**", "/", "/products/**", "/api/products/**", "/css/**", "/js/**", "/images/**", "/product-images/**", "/login", "/login/google/link", "/oauth2/authorization/google", "/login/oauth2/code/google", "/register", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/support", "/support/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .formLogin(form -> form.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/",false).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/?logout"))
            .addFilterBefore(new LoginThrottleFilter(loginLimit),org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AccountStateFilter(users),org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .build(); // CSRF protection and session fixation protection remain enabled.
    }
}
