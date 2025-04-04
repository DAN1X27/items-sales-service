package danix.app.announcements_service.task;

import danix.app.announcements_service.repositories.AnnouncementsRepository;
import danix.app.announcements_service.services.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeleteExpiredAnnouncementsTask {

    private final AnnouncementService announcementService;
    private final AnnouncementsRepository announcementsRepository;
    @Value("${max_storage_days}")
    private int maxStorageDays;

    @Scheduled(cron = "@midnight")
    public void run() {
        announcementsRepository.findAllByCreatedAtBefore(LocalDateTime.now().minusDays(maxStorageDays)).forEach(announcement ->
                announcementService.delete(announcement.getId()));
    }
}
