package danix.app.chats_service.models;

import danix.app.chats_service.security.UserDetailsServiceImpl;
import danix.app.chats_service.util.ContentType;

import java.time.LocalDateTime;

public class UsersChatFactory extends ChatFactory {

    @Override
    public Chat createChat(Long user1Id, Long user2Id) {
        return new UsersChat(user1Id, user2Id);
    }

    @Override
    public Message createMessage(String text, Chat chat, ContentType contentType) {
        return ChatMessage.builder()
                .chat((UsersChat) chat)
                .senderId(UserDetailsServiceImpl.getCurrentUser().getId())
                .text(text)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .build();
    }
}
