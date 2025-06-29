package danix.app.announcements_service.feign;

import danix.app.announcements_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "files-service", configuration = FeignConfig.class)
public interface FilesAPI {

	String PATH = "/announcement/image";

	@PostMapping(value = PATH, consumes = "multipart/form-data")
	void saveImage(@RequestPart("image") MultipartFile image, @RequestParam String fileName,
				   @RequestParam("access_key") String accessKey);

	@GetMapping(value = PATH)
	byte[] downloadImage(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

	@DeleteMapping(PATH)
	void deleteImage(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

}