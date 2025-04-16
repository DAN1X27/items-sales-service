package danix.app.chats_service.feign;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "files-service", configuration = FeignConfig.class)
public interface FilesService {

	String IMAGE_PATH = "/chat/image";

	String VIDEO_PATH = "/chat/video";

	@PostMapping(value = IMAGE_PATH, consumes = "multipart/form-data")
	void saveImage(@RequestPart MultipartFile image, @RequestParam String fileName,
			@RequestParam("access_key") String accessKey);

	@GetMapping(value = IMAGE_PATH, produces = "image/jpeg")
	byte[] downloadImage(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

	@DeleteMapping(IMAGE_PATH)
	void deleteImage(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

	@PostMapping(value = VIDEO_PATH, consumes = "multipart/form-data")
	void saveVideo(@RequestPart MultipartFile video, @RequestParam String fileName,
			@RequestParam("access_key") String accessKey);

	@GetMapping(value = VIDEO_PATH, produces = "video/mp4")
	byte[] downloadVideo(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

	@DeleteMapping(VIDEO_PATH)
	void deleteVideo(@RequestParam String fileName, @RequestParam("access_key") String accessKey);

}
