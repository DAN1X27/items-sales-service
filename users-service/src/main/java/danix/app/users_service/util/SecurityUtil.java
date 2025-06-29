package danix.app.users_service.util;

import danix.app.users_service.models.User;
import danix.app.users_service.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UsersRepository usersRepository;

    public User getCurrentUser() {
        JwtAuthenticationToken authToken = getAuthToken();
        Jwt jwt = authToken.getToken();
        long userId = jwt.getClaim("user_id");
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public String getJwtBearerToken() {
        return "Bearer " + getAuthToken().getToken().getTokenValue();
    }

    private JwtAuthenticationToken getAuthToken() {
        return (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    }

}
