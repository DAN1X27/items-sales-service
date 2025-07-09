package danix.app.chats_service.util;

import danix.app.chats_service.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SecurityUtil {

    public User getCurrentUserFromAuthentication(Authentication authentication) {
        return convertJwtToUser((JwtAuthenticationToken) authentication);
    }

    public User getCurrentUser() {
        JwtAuthenticationToken authToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return convertJwtToUser(authToken);
    }

    public String getJwt() {
        JwtAuthenticationToken authToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return "Bearer " + authToken.getToken().getTokenValue();
    }

    private User convertJwtToUser(JwtAuthenticationToken authToken) {
        Jwt jwt = authToken.getToken();
        return User.builder()
                .username(jwt.getClaim(authToken.getName()))
                .id(jwt.getClaim("user_id"))
                .email(jwt.getClaim("email"))
                .firstName(jwt.getClaim("first_name"))
                .lastName(jwt.getClaim("last_name"))
                .country(jwt.getClaim("country"))
                .city(jwt.getClaim("city"))
                .build();
    }

}
