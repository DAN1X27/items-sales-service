package danix.app.authentication_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthConverter implements Converter<Jwt, JwtAuthenticationToken> {

    @Value("${jwt.principal-attribute}")
    private String principalAttribute;

    @Value("${jwt.resource-id}")
    private String resourceId;

    @Override
    public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<? extends GrantedAuthority> roles = extractRoles(jwt);
        return new JwtAuthenticationToken(
                jwt,
                roles,
                jwt.getClaim(principalAttribute)
        );
    }

    private Collection<? extends GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> resourceAccess;
        Map<String, Object> resource;
        Collection<String> roles;
        if (jwt.getClaim("resource_access") == null) {
            return Set.of();
        }
        resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess.get(resourceId) == null) {
            return Set.of();
        }
        resource = (Map<String, Object>) resourceAccess.get(resourceId);
        roles = (Collection<String>) resource.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
