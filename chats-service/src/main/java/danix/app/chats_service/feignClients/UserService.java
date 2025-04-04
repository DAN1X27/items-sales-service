package danix.app.chats_service.feignClients;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UserService {
    @GetMapping("/user/{id}")
    Map<String, Object> getById(@PathVariable long id, @RequestHeader("Authorization") String token);
}
