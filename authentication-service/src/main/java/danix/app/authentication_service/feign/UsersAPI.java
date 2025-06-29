package danix.app.authentication_service.feign;

import danix.app.authentication_service.config.FeignConfig;
import danix.app.authentication_service.dto.TempRegistrationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UsersAPI {

    @PostMapping("/users/registration")
    void tempRegistration(@RequestBody TempRegistrationDTO registrationDTO, @RequestParam("access_key") String key);

    @PatchMapping("/users/registration/confirm")
    Map<String, Long> registrationConfirm(@RequestParam String email, @RequestParam("access_key") String key);

    @PatchMapping("/users/password/reset")
    void resetPassword(@RequestParam String email, @RequestParam String password,
                       @RequestParam("access_key") String accessKey);

    @PatchMapping("/users/email")
    void updateEmail(@RequestParam String email, @RequestParam("access_key") String accessKey,
                     @RequestHeader(HttpHeaders.AUTHORIZATION) String token);

    @DeleteMapping("/users/temp")
    void deleteTempUser(@RequestParam String email, @RequestParam("access_key") String accessKey);

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable Long id, @RequestParam("access_key") String accessKey);

    @GetMapping("/users/is-banned")
    Map<String, Object> isUserBanned(@RequestParam String username, @RequestParam("access_key") String accessKey);

}
