package danix.app.announcements_service.security;

import danix.app.announcements_service.feignClients.AuthenticationService;
import lombok.RequiredArgsConstructor;
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
}
