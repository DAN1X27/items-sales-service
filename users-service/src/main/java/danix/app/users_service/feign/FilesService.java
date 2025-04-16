package danix.app.users_service.feign;

import danix.app.users_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "files-service", configuration = FeignConfig.class)
public interface FilesService {

	String PATH = "/user/avatar";

	@PutMapping(value = PATH, consumes = "multipart/form-data")
	void updateAvatar(@RequestPart("image") MultipartFile image, @RequestParam String fileName,
			@RequestParam("access_key") String accessKey);

	@GetMapping(value = PATH, produces = "image/jpeg")
	byte[] downloadAvatar(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

	@DeleteMapping(PATH)
	void deleteAvatar(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

}
