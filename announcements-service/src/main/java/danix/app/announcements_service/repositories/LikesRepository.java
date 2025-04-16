package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikesRepository extends JpaRepository<Like, Long> {

	Optional<Like> findByAnnouncementAndUserId(Announcement announcement, Long userId);

}
