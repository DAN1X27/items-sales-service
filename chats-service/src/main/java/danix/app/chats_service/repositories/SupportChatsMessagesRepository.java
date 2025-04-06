package danix.app.chats_service.repositories;

import danix.app.chats_service.models.SupportChat;
import danix.app.chats_service.models.SupportChatMessage;
import danix.app.chats_service.util.ContentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportChatsMessagesRepository extends JpaRepository<SupportChatMessage, Long> {
    List<SupportChatMessage> findAllByChatAndContentType(SupportChat chat, ContentType contentType);

    List<SupportChatMessage> findAllByChat(SupportChat chat, Pageable pageable);
}
