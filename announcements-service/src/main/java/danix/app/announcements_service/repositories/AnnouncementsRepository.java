package danix.app.announcements_service.repositories;

import danix.app.announcements_service.models.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementsRepository extends JpaRepository<Announcement, Long> {

	@Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.images WHERE a.id IN (:ids)")
	List<Announcement> findAllByIdIn(List<Long> ids, Sort sort);

	List<IdProjection> findAllByOwnerId(Long ownerId, Pageable pageable);

	List<IdProjection> findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(String title, String country,
			String city, List<String> types, Pageable pageable);

	List<IdProjection> findAllByCreatedAtBefore(LocalDateTime time, Pageable pageable);

	List<IdProjection> findAllByCountryAndCityAndTypeIn(String county, String city, Pageable pageable,
			List<String> types);

	List<IdProjection> findAllByCountryAndCity(String country, String city, Pageable pageable);

	List<IdProjection> findAllByTitleContainsIgnoreCaseAndCountryAndCity(String title, String country, String city,
			Pageable pageable);

	@Modifying
	@Query("DELETE FROM Announcement a WHERE a.id IN (:ids)")
	void deleteAllByIdIn(List<Long> ids);

	@Modifying
	@Query("DELETE FROM Announcement a WHERE a.id = :id")
	void deleteById(Long id);
}