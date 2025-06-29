package danix.app.chats_service.feign;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UsersAPI {

	@GetMapping("/users/{id}/is-blocked")
	Map<String, Boolean> isBlockedByUser(@PathVariable long id, @RequestHeader(AUTHORIZATION) String jwt);

}
