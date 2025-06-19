package danix.app.authentication_service.util;

import java.time.LocalDateTime;

public record ErrorData(String message, LocalDateTime timestamp) {
}
