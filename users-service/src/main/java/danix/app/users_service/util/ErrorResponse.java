package danix.app.users_service.util;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

	private final String message;

	private final LocalDateTime timestamp;

}
