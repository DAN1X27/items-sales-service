package danix.app.announcements_service.task;

import danix.app.announcements_service.repositories.AnnouncementsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeleteExpiredAnnouncementsTask {

	private final AnnouncementsRepository announcementsRepository;

	@Value("${max_storage_days}")
	private int maxStorageDays;

	@Scheduled(cron = "@midnight")
	@Transactional
	public void run() {
		announcementsRepository
			.deleteAll(announcementsRepository.findAllByCreatedAtBefore(LocalDateTime.now().minusDays(maxStorageDays)));
	}

}
