package danix.app.chats_service.dto;

import danix.app.chats_service.util.ErrorCode;
import danix.app.chats_service.util.ErrorData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponseDTO {

    private final int code;

    private final ErrorData error;

    public ErrorResponseDTO(ErrorCode code, ErrorData error) {
        this.code = code.getCode();
        this.error = error;
    }
}
