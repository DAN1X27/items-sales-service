package danix.app.chats_service.feignClients;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@LoadBalancerClient
@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {
    @GetMapping("/auth/authorize")
    Map<String, Object> authorize(@RequestHeader("Authorization") String token);
}
