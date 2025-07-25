package danix.app.authentication_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("keycloak")
@Data
public class KeycloakProperties {

    private String clientId;

    private String clientSecret;

    private String adminClientId;

    private String adminClientSecret;
}
