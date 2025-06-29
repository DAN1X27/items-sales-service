package danix.app.tasks_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "announcements-service")
public interface AnnouncementsAPI {

    @DeleteMapping("/announcements/expired")
    void deleteExpiredAnnouncements(@RequestParam("access_key") String accessKey);

}
