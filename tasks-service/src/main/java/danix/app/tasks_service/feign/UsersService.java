package danix.app.tasks_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users-service")
public interface UsersService {

    @DeleteMapping("/users/temp")
    void deleteTempUsers(@RequestParam("access_key") String accessKey);

}
