package danix.app.announcements_service.feign;

import danix.app.announcements_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UsersAPI {

    @GetMapping("/users/{id}/email")
    Map<String, Object> getUserEmail(@PathVariable long id, @RequestParam("access_key") String accessKey);
}
