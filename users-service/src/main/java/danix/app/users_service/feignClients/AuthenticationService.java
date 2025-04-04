package danix.app.users_service.feignClients;

import danix.app.users_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {
    @GetMapping("/auth/authorize")
    Map<String, Object> authorize(@RequestHeader("Authorization") String token);

    @DeleteMapping("/auth/tokens/{id}")
    void deleteUserTokens(@RequestHeader("Authorization") String token, @PathVariable Long id);
}
