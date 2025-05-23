package danix.app.chats_service.models;

import danix.app.chats_service.security.UserDetailsServiceImpl;
import danix.app.chats_service.util.ContentType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UsersChatFactory implements ChatFactory {

    @Override
    public UsersChat getChat(Long user1Id, Long user2Id) {
        return UsersChat.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
    }

    @Override
    public ChatMessage getMessage(String text, Chat chat, ContentType contentType) {
        return ChatMessage.builder()
                .chat((UsersChat) chat)
                .senderId(UserDetailsServiceImpl.getCurrentUser().getId())
                .text(text)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatType getChatType() {
        return ChatType.USERS_CHAT;
    }
}
