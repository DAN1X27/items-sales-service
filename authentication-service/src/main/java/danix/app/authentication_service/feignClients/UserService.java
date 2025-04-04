package danix.app.authentication_service.feignClients;

import danix.app.authentication_service.config.FeignConfig;
import danix.app.authentication_service.dto.RegistrationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UserService {
    @GetMapping("/user/authentication")
    Map<String, Object> getUserAuthentication(@RequestParam String email);

    @PostMapping("/user/registration")
    void tempRegistration(@RequestBody RegistrationDTO registrationDTO);

    @PostMapping("/user/registration-confirm")
    Long registrationConfirm(@RequestParam String email);

    @GetMapping("/user/info")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String token);
}
