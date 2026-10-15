package vn.sunicoffee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;

@Configuration
@ConditionalOnProperty(name="app.google.enabled",havingValue="true")
public class GoogleLoginConfig {
    @Bean ClientRegistrationRepository googleClients(@Value("${app.google.client-id}") String id,@Value("${app.google.client-secret}") String secret) {
        if(id.isBlank() || secret.isBlank()) throw new IllegalArgumentException("Google Client ID/Secret chưa được cấu hình.");
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(id).clientSecret(secret).scope("openid","email","profile")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").build());
    }
}
