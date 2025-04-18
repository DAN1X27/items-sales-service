package danix.app.authentication_service.feign;

import danix.app.authentication_service.config.FeignConfig;
import danix.app.authentication_service.dto.RegistrationDTO;
import danix.app.authentication_service.security.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "users-service", configuration = FeignConfig.class)
public interface UsersService {

	@GetMapping("/users/authentication")
    User getUserAuthentication(@RequestParam String email, @RequestParam("access_key") String access_key);

	@PostMapping("/users/registration")
	void tempRegistration(@RequestBody RegistrationDTO registrationDTO, @RequestParam("access_key") String key);

	@PatchMapping("/users/registration/confirm")
	Map<String, Object> registrationConfirm(@RequestParam String email, @RequestParam("access_key") String key);

	@PatchMapping("/users/password/reset")
	void resetPassword(@RequestParam String email, @RequestParam String password,
			@RequestParam("access_key") String accessKey);

	@PatchMapping("/users/email")
	void updateEmail(@RequestParam String email, @RequestParam("access_key") String accessKey,
			@RequestHeader("Authorization") String token);

}
