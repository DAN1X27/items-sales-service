package danix.app.users_service.repositories;

import danix.app.users_service.models.Grade;
import danix.app.users_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradesRepository extends JpaRepository<Grade, Long> {

	Optional<Grade> findByUserAndOwner(User user, User owner);

	@Query(value = "SELECT AVG(stars) AS average_grade FROM grades where grades.user_id = :user_id", nativeQuery = true)
	double getAverageGrade(@Param("user_id") Long userId);
}
