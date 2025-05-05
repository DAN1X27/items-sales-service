package danix.app.chats_service.models;

import danix.app.chats_service.security.UserDetailsServiceImpl;
import danix.app.chats_service.util.ContentType;

import java.time.LocalDateTime;

class UsersChatFactory implements AbstractChatFactory {

    @Override
    public Chat getChat(Long user1Id, Long user2Id) {
        return UsersChat.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
    }

    @Override
    public Message getMessage(String text, Chat chat, ContentType contentType) {
        return ChatMessage.builder()
                .chat((UsersChat) chat)
                .senderId(UserDetailsServiceImpl.getCurrentUser().getId())
                .text(text)
                .contentType(contentType)
                .sentTime(LocalDateTime.now())
                .build();
    }
}
