package danix.app.users_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${access_key}")
    private String accessKey;

    @Value("${allowed_origins}")
    private List<String> allowedOrigins;

    private final JwtAuthConverter authConverter;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfiguration()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error", "/users/swagger-ui/**", "/users/v3/api-docs")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS)
                        .permitAll()
                        .requestMatchers("/users/registration", "/users/registration/confirm", "/users/{id}/email",
                                "/users/email", "/users/temp", "/users/is-banned")
                        .access(accessKeyAuthManager())
                        .requestMatchers(HttpMethod.DELETE, "/users/{id}")
                        .access(accessKeyAuthManager())
                        .requestMatchers("/users/reports", "/users/report/", "/users/{id}/ban", "/users/{id}/unban",
                                "/users/banned")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .hasAnyRole("USER", "ADMIN"))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(authConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> accessKeyAuthManager() {
        return (authentication, object) -> {
            String accessKey = object.getRequest().getParameter("access_key");
            if (accessKey == null || !accessKey.equals(this.accessKey)) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(true);
        };
    }

}
