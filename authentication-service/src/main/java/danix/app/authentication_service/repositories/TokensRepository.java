package danix.app.authentication_service.repositories;

import danix.app.authentication_service.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface TokensRepository extends JpaRepository<Token, String> {

    @Modifying
    void deleteAllByUserId(Long id);

    @Modifying
    void deleteAllByExpiredDateBefore(Date date);
}
