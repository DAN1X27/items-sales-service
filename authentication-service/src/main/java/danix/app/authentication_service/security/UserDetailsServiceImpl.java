package danix.app.authentication_service.security;

import danix.app.authentication_service.feign.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UsersService usersService;

	@Value("${access_key}")
	private String accessKey;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User authentication = usersService.getUserAuthentication(email, accessKey);
		return new UserDetailsImpl(authentication);
	}

}