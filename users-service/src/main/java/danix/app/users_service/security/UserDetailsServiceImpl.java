package danix.app.users_service.security;

import danix.app.users_service.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UsersService usersService;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return new UserDetailsImpl(usersService.getByEmail(email));
	}

}
