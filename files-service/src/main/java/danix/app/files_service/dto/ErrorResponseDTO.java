package danix.app.files_service.dto;

import danix.app.files_service.util.ErrorData;
import lombok.Data;

@Data
public class ErrorResponseDTO {

	private final int code = 101;

	private final ErrorData error;

}