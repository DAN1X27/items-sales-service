package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;

public interface AbstractChatFactory {

    Chat getChat(Long user1Id, Long user2Id);

    Message getMessage(String text, Chat chat, ContentType contentType);

    static AbstractChatFactory getFactory(ChatType chatType) {
        return switch (chatType) {
            case USERS_CHAT -> new UsersChatFactory();
            case SUPPORT_CHAT -> new SupportChatFactory();
        };
    }

}
