package danix.app.authentication_service.repositories;

import danix.app.authentication_service.models.Token;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface TokensRepository extends CrudRepository<Token, String> {

	@Modifying
	@Query("DELETE FROM tokens where user_id = :id")
	void deleteAllByUserId(@Param("id") Long id);

	@Modifying
	@Query("DELETE FROM tokens where expired_date <= :date")
	void deleteAllByExpiredDateBefore(@Param("date") Date date);

}
