package danix.app.tasks_service.tasks;

import danix.app.tasks_service.feign.AnnouncementsAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExpiredAnnouncementsTask {

    private final AnnouncementsAPI announcementsAPI;

    @Value("${access_key}")
    private String accessKey;

    @Scheduled(cron = "@midnight")
    public void run() {
        log.info("Start deleting expired announcements...");
        try {
            announcementsAPI.deleteExpiredAnnouncements(accessKey);
            log.info("Expired announcements successfully deleted.");
        }
        catch (Exception e) {
            log.error("Error deleting expired announcements: {}", e.getMessage(), e);
        }
    }

}
