package danix.app.users_service.feignClients;

import danix.app.users_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "files-service", configuration = FeignConfig.class)
public interface FilesService {
    String PATH = "/user/avatar";

    @PutMapping(value = PATH, consumes = "multipart/form-data")
    void updateAvatar(@RequestPart("image") MultipartFile image, @RequestParam String fileName);

    @GetMapping(PATH)
    Map<String, Object> downloadAvatar(@RequestParam String fileName);

    @DeleteMapping(PATH)
    void deleteAvatar(@RequestParam String fileName);
}
