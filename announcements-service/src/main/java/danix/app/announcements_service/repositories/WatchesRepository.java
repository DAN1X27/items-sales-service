package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.Watch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchesRepository extends JpaRepository<Watch, Long> {

	Optional<Watch> findByAnnouncementAndUserId(Announcement announcement, Long userId);

}
