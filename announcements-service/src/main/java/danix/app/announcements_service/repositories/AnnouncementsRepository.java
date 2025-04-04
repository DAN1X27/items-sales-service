package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementsRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOwnerId(Long id);

    List<Announcement> findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(String title, String country,
                                                             String city, List<String> types, Pageable pageable);

    List<Announcement> findAllByCreatedAtBefore(LocalDateTime time);

    List<Announcement> findAllByCountryAndCityAndTypeIn(String county, String city, Pageable pageable, List<String> types);

    List<Announcement> findAllByCountryAndCity(String country, String city, Pageable pageable);

    List<Announcement> findAllByTitleContainsIgnoreCaseAndCountryAndCity(String title, String country,
                                                                         String city, Pageable pageable);
}