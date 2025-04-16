package danix.app.users_service.repositories;

import danix.app.users_service.models.Comment;
import danix.app.users_service.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comment, Long> {

	List<Comment> findAllByUser(User user, Pageable pageable);

}
