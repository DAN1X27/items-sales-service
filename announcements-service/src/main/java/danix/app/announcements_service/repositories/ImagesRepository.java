package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<Image, Long> {

}
