package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.AnnouncementImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementsImagesRepository extends JpaRepository<AnnouncementImage, Long> {
}
