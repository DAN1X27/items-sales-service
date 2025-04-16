package danix.app.announcements_service.feign;

import danix.app.announcements_service.config.FeignConfig;
import danix.app.announcements_service.security.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {

	@GetMapping("/auth/authorize")
	User authorize(@RequestHeader("Authorization") String token);

}