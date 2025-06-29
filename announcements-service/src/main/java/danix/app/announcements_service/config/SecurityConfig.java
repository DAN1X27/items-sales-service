package danix.app.announcements_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthConverter authConverter;

    @Value("${access_key}")
    private String accessKey;

    @Value("${allowed_origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain config(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfiguration()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error", "/announcements/swagger-ui/**", "/announcements/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/announcements", "/announcements/{id}", "/announcements/find")
                        .permitAll()
                        .requestMatchers("/announcements/reports", "/announcements/report/{id}",
                                "/announcements/{id}/ban")
                        .hasRole("ADMIN")
                        .requestMatchers("/announcements/expired")
                        .access(accessKeyAuthManager())
                        .anyRequest()
                        .hasAnyRole("USER", "ADMIN"))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authConverter)))
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