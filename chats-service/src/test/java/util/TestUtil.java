package util;

import danix.app.chats_service.models.SupportChat;
import danix.app.chats_service.models.User;
import danix.app.chats_service.models.UsersChat;

public final class TestUtil {

    private static final long CURRENT_USER_ID = 1L;

    private static final long USER_ID = 2L;

    private TestUtil() {
    }

    public static User getTestCurrentUser() {
        return User.builder()
                .id(CURRENT_USER_ID)
                .username("current_user")
                .firstName("First name")
                .lastName("Last name")
                .email("current_user@gmail.com")
                .country("country")
                .city("city")
                .build();
    }

    public static UsersChat getTestUsersChat() {
        return UsersChat.builder()
                .user1Id(CURRENT_USER_ID)
                .user2Id(USER_ID)
                .build();
    }

    public static SupportChat getTestSupportChat() {
        return SupportChat.builder()
                .userId(CURRENT_USER_ID)
                .status(SupportChat.Status.WAIT)
                .build();
    }

}
