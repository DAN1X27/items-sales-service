package danix.app.users_service.repositories;

import danix.app.users_service.models.BannedUser;
import danix.app.users_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannedUsersRepository extends JpaRepository<BannedUser, Integer> {

	Optional<BannedUser> findByUser(User user);

	@Query("SELECT bu FROM BannedUser bu where bu.user.username = :username")
	Optional<BannedUser> findByEmail(String username);

	@Query("SELECT bu FROM BannedUser bu where bu.user.email = :email")
	Optional<BannedUser> findByUsername(String email);
}
