package danix.app.users_service.feignClients;

import danix.app.users_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationService {
    @GetMapping("/auth/authorize")
    Map<String, Object> authorize(@RequestHeader("Authorization") String token);

    @DeleteMapping("/auth/tokens/{id}")
    void deleteUserTokens(@RequestHeader("Authorization") String token, @PathVariable Long id);

    @PostMapping("/auth/key")
    void sendEmailKey(@RequestParam String email, @RequestParam String message,
                      @RequestHeader("Authorization") String token);

    @PostMapping("/auth/email/update")
    Map<String, Object> validateEmailKey(@RequestBody Map<String, Object> emailKey,
                                         @RequestHeader("Authorization") String token);
}