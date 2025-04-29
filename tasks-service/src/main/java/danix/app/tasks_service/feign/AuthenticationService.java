package danix.app.tasks_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "authentication-service")
public interface AuthenticationService {

    @DeleteMapping("/auth/tokens/expired")
    void deleteExpiredTokens(@RequestParam("access_key") String accessKey);

}
