package danix.app.users_service.repositories;

import danix.app.users_service.models.Grade;
import danix.app.users_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradesRepository extends JpaRepository<Grade, Long> {

	Optional<Grade> findByUserAndOwner(User user, User owner);

}
