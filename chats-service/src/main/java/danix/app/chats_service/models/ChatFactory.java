package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;

public interface ChatFactory {

    <C extends Chat> C getChat(Long user1Id, Long user2Id);

    <M extends Message> M getMessage(String text, Chat chat, ContentType contentType);

    ChatType getChatType();

}
