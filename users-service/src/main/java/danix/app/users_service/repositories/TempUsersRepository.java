package danix.app.users_service.repositories;

import danix.app.users_service.models.TempUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempUsersRepository extends CrudRepository<TempUser, String> {
}
