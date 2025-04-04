package danix.app.chats_service.feignClients;

import danix.app.chats_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "files-service", configuration = FeignConfig.class)
public interface FilesService {
    String path = "/chat/image";

    @PostMapping(value = path, consumes = "multipart/form-data")
    void saveImage(@RequestPart MultipartFile image, @RequestParam String fileName);

    @GetMapping(path)
    Map<String, Object> downloadImage(@RequestParam String fileName);

    @DeleteMapping(path)
    void deleteImage(@RequestParam String fileName);
}
