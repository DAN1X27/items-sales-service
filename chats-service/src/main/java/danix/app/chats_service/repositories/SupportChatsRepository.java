package danix.app.chats_service.repositories;

import danix.app.chats_service.models.SupportChat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportChatsRepository extends JpaRepository<SupportChat, Long> {

	Optional<SupportChat> findByUserIdAndStatusIn(long userId, List<SupportChat.Status> statuses);

	List<SupportChat> findAllByUserIdOrAdminId(long userId, long adminId, Pageable pageable);

	List<SupportChat> findAllByStatus(SupportChat.Status status, Pageable pageable);

	@Modifying
	@Query("delete from SupportChat c where c.id = :id")
	void deleteById(@Param("id") long id);

}