package danix.app.chats_service.repositories;

import danix.app.chats_service.models.Chat;
import danix.app.chats_service.models.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagesRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByChatAndContentType(Chat chat, Message.ContentType contentType);

    List<Message> findAllByChat(Chat chat, Pageable pageable);
}
