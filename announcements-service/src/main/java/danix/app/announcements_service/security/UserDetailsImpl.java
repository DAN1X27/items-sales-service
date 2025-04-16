package danix.app.announcements_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public record UserDetailsImpl(User authentication) implements UserDetails {

	public User authentication() {
		return authentication;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singleton(new SimpleGrantedAuthority(authentication.getRole()));
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return authentication.getEmail();
	}
}
