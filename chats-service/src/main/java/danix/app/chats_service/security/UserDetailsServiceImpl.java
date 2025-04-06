package danix.app.chats_service.security;

import danix.app.chats_service.feignClients.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthenticationService authenticationService;

    @Override
    public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {
        Map<String, Object> data = authenticationService.authorize(token);
        return new UserDetailsImpl(User.builder()
                .id(Long.parseLong(String.valueOf(data.get("id"))))
                .email((String) data.get("email"))
                .role((String) data.get("role"))
                .city((String) data.get("city"))
                .country((String) data.get("country"))
                .build());
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.authentication();
    }
}
