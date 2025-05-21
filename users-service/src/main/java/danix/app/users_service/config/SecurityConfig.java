package danix.app.users_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;

    @Value("${access_key}")
    private String accessKey;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers("/users/registration", "/users/registration/confirm", "/users/authentication",
                                "/users/password/reset", "/users/{id}/email", "/users/email", "/users/temp")
                        .access(accessKeyAuthManager())
                        .requestMatchers("/users/email")
                        .hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/users/reports", "/users/report/", "/users/{id}/ban", "/users/{id}/unban",
                                "/users/banned")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .hasAnyRole("USER", "ADMIN"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthorizationManager<RequestAuthorizationContext> accessKeyAuthManager() {
        return (authentication, object) -> {
            String accessKey = object.getRequest().getParameter("access_key");
            if (accessKey == null || !accessKey.equals(this.accessKey)) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(true);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
