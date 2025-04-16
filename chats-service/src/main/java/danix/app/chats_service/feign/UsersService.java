package danix.app.chats_service.feign;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UsersService {

	@GetMapping("/users/{id}/is-blocked")
	Map<String, Object> isBlocked(@PathVariable long id, @RequestHeader("Authorization") String token);

}
