package danix.app.users_service.feign;

import danix.app.users_service.config.FeignConfig;
import danix.app.users_service.dto.UpdateInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "authentication-service", configuration = FeignConfig.class)
public interface AuthenticationAPI {

	@PutMapping("/auth/user")
	void updateUserInfo(@RequestBody UpdateInfoDTO updateInfoDTO, @RequestHeader(AUTHORIZATION) String jwt,
						@RequestParam("access_key") String accessKey);

	@PatchMapping("/auth/disable-user")
	void disableUser(@RequestParam String email);

	@PatchMapping("/auth/enable-user")
	void enableUser(@RequestParam String email);

}