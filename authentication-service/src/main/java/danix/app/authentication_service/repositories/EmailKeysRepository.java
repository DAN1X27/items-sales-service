package danix.app.authentication_service.repositories;

import danix.app.authentication_service.models.EmailKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailKeysRepository extends CrudRepository<EmailKey, String> {
}
