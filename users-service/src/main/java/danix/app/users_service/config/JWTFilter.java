package danix.app.users_service.config;

import danix.app.users_service.security.UserDetailsImpl;
import danix.app.users_service.security.UserDetailsServiceImpl;
import danix.app.users_service.feign.AuthenticationService;
import feign.FeignException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final AuthenticationService authenticationService;

	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			try {
				Map<String, Object> data = authenticationService.authorize(authHeader);
				String email = (String) data.get("email");
				UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
				var authToken = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
				);
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} catch (FeignException.Unauthorized e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

}