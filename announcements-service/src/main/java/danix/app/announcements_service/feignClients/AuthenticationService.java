package danix.app.announcements_service.feignClients;

import danix.app.announcements_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {
    @GetMapping("/auth/authorize")
    Map<String, Object> authorize(@RequestHeader("Authorization") String token);
}
