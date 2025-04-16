package danix.app.announcements_service.security;

import danix.app.announcements_service.feign.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final AuthenticationService authenticationService;

	@Override
	public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {
		return new UserDetailsImpl(authenticationService.authorize(token));
	}

}
