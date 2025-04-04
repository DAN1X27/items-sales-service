package danix.app.users_service.repositories;

import danix.app.users_service.models.BlockedUser;
import danix.app.users_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedUsersRepository extends JpaRepository<BlockedUser, Long> {
    Optional<BlockedUser> findByOwnerAndUser(User owner, User user);
}
