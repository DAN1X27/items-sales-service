package danix.app.files_service.util;

import java.time.LocalDateTime;

public record ErrorData(String message, LocalDateTime timestamp) {
}
