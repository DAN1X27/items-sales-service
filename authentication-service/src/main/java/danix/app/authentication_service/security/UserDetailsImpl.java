package danix.app.authentication_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public record UserDetailsImpl(UserAuthentication userAuthentication) implements UserDetails {

    public UserAuthentication userAuthentication() {
        return userAuthentication;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(userAuthentication.getRole()));
    }

    @Override
    public String getPassword() {
        return userAuthentication.getPassword();
    }

    @Override
    public String getUsername() {
        return userAuthentication.getEmail();
    }
}
