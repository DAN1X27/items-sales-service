package danix.app.announcements_service.feignClients;

import danix.app.announcements_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "files-service", configuration = FeignConfig .class)
public interface FilesService {
    String PATH = "/announcement/image";

    @PostMapping(value = PATH, consumes = "multipart/form-data")
    void addImage(@RequestPart("image") MultipartFile image, @RequestParam String fileName);

    @GetMapping(PATH)
    Map<String, Object> downloadImage(@RequestParam String fileName);

    @DeleteMapping(PATH)
    void deleteImage(@RequestParam String fileName);
}