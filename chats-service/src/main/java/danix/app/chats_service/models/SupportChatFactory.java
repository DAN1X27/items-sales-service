package danix.app.chats_service.models;

import danix.app.chats_service.security.UserDetailsServiceImpl;
import danix.app.chats_service.util.ContentType;

import java.time.LocalDateTime;

class SupportChatFactory extends ChatFactory {

    @Override
    public Chat createChat(Long user1Id, Long user2Id) {
        return new SupportChat(user1Id, SupportChat.Status.WAIT);
    }

    @Override
    public Message createMessage(String text, Chat chat, ContentType contentType) {
        return SupportChatMessage.builder()
                .text(text)
                .chat((SupportChat) chat)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .senderId(UserDetailsServiceImpl.getCurrentUser().getId())
                .build();
    }
}
