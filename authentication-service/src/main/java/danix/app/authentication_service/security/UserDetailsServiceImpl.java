package danix.app.authentication_service.security;

import danix.app.authentication_service.feignClients.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Map<String, Object> authenticationData = userService.getUserAuthentication(email);
        return new UserDetailsImpl(
                UserAuthentication.builder()
                        .email((String) authenticationData.get("email"))
                        .password((String) authenticationData.get("password"))
                        .id(Long.parseLong(String.valueOf(authenticationData.get("id"))))
                        .role((String) authenticationData.get("role"))
                        .build()
        );
    }
}