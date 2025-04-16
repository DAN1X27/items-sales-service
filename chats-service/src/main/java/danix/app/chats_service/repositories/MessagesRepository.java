package danix.app.chats_service.repositories;

import danix.app.chats_service.models.Chat;
import danix.app.chats_service.models.ChatMessage;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.util.ContentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagesRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByChatAndContentTypeIn(Chat chat, List<ContentType> contentType);

	List<Message> findAllByChat(Chat chat, Pageable pageable);

}
