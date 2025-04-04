package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByAnnouncementAndUserId(Announcement announcement, Long userId);
}
