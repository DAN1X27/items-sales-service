package danix.app.announcements_service.util;

import danix.app.announcements_service.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    public User getCurrentUser() {
        JwtAuthenticationToken authToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = authToken.getToken();
        return User.builder()
                .id(jwt.getClaim("user_id"))
                .username(authToken.getName())
                .email(jwt.getClaim("email"))
                .firstName(jwt.getClaim("first_name"))
                .lastName(jwt.getClaim("last_name"))
                .city(jwt.getClaim("city"))
                .country(jwt.getClaim("country"))
                .build();
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

}
