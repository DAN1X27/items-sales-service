package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;

public abstract class ChatFactory {

    public abstract Chat createChat(Long user1Id, Long user2Id);

    public abstract Message createMessage(String text, Chat chat, ContentType contentType);

    public static ChatFactory getFactory(FactoryType type) {
        return switch (type) {
            case USERS_CHAT_FACTORY -> new UsersChatFactory();
            case SUPPORT_CHAT_FACTORY -> new SupportChatFactory();
        };
    }

}
