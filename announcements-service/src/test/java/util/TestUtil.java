package util;

import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.User;

import java.time.LocalDateTime;

public final class TestUtil {

    private TestUtil() {
    }

    public static User getTestUser() {
        return User.builder()
                .id(1L)
                .username("test")
                .email("test")
                .firstName("test")
                .lastName("test")
                .country("test")
                .city("test")
                .build();
    }

    public static Announcement getTestAnnouncement() {
        return Announcement.builder()
                .id(1L)
                .ownerId(getTestUser().getId())
                .title("test")
                .description("test")
                .country("test")
                .city("test")
                .price(100.0)
                .type("test")
                .phoneNumber("test")
                .createdAt(LocalDateTime.now())
                .build();
    }

}
