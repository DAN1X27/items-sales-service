package danix.app.users_service.repositories;

import danix.app.users_service.models.Report;
import danix.app.users_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Report, Integer> {
    Optional<Report> findByUserAndSender(User user, User sender);
}
