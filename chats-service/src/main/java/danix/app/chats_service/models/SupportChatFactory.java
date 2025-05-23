package danix.app.chats_service.models;

import danix.app.chats_service.security.UserDetailsServiceImpl;
import danix.app.chats_service.util.ContentType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
class SupportChatFactory implements ChatFactory {

    @Override
    public SupportChat getChat(Long user1Id, Long user2Id) {
        return SupportChat.builder()
                .userId(user1Id)
                .status(SupportChat.Status.WAIT)
                .build();
    }

    @Override
    public SupportChatMessage getMessage(String text, Chat chat, ContentType contentType) {
        return SupportChatMessage.builder()
                .text(text)
                .chat((SupportChat) chat)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .senderId(UserDetailsServiceImpl.getCurrentUser().getId())
                .build();
    }

    @Override
    public ChatType getChatType() {
        return ChatType.SUPPORT_CHAT;
    }
}
