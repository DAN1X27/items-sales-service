package danix.app.chats_service.util;

import java.time.LocalDateTime;

public record ErrorData(String message, LocalDateTime timestamp) {
}
