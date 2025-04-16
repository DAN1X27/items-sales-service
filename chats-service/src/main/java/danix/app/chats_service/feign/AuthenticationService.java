package danix.app.chats_service.feign;

import danix.app.chats_service.config.FeignConfig;
import danix.app.chats_service.security.User;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@LoadBalancerClient
@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {

	@GetMapping("/auth/authorize")
	User authorize(@RequestHeader("Authorization") String token);

}
