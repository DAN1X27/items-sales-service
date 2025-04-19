package danix.app.chats_service.repositories;

import danix.app.chats_service.models.UsersChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatsRepository extends JpaRepository<UsersChat, Long> {

	List<UsersChat> findAllByUser1IdOrUser2Id(long user1Id, long user2Id);

	Optional<UsersChat> findByUser1IdAndUser2Id(long user1Id, long user2Id);

	Optional<UsersChat> findById(long id);

	@Modifying
	@Query("delete from UsersChat c where c.id = :id")
	void deleteById(@Param("id") long id);

}