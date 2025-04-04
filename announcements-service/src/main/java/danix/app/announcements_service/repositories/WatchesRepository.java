package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.AnnouncementWatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchesRepository extends JpaRepository<AnnouncementWatch, Long> {
    Optional<AnnouncementWatch> findByAnnouncementAndUserId(Announcement announcement, Long userId);
}
