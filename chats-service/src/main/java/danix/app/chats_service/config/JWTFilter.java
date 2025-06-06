package danix.app.chats_service.config;

import danix.app.chats_service.security.UserDetailsImpl;
import danix.app.chats_service.security.UserDetailsServiceImpl;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			try {
				UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(authHeader);
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
