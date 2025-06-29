package danix.app.users_service.feign;

import danix.app.users_service.config.FeignConfig;
import danix.app.users_service.dto.UpdateInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationAPI {

	@PutMapping("/auth/user")
	void updateUserInfo(@RequestBody UpdateInfoDTO updateInfoDTO, @RequestHeader(AUTHORIZATION) String jwt,
						@RequestParam("access_key") String accessKey);

	@DeleteMapping("/auth/user")
	void deleteUser(@RequestParam("access_key") String accessKey, @RequestHeader(AUTHORIZATION) String jwt);

	@PatchMapping("/auth/disable-user")
	void disableUser(@RequestParam String email);

	@PatchMapping("/auth/enable-user")
	void enableUser(@RequestParam String email);

}