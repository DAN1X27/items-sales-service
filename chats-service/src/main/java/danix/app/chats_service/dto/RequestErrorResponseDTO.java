package danix.app.chats_service.dto;

import danix.app.chats_service.util.ErrorCode;
import danix.app.chats_service.util.ErrorData;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RequestErrorResponseDTO {

    private final int code;

    private final Map<String, ErrorData> error;

    public RequestErrorResponseDTO(ErrorCode code, Map<String, ErrorData> error) {
        this.code = code.getCode();
        this.error = error;
    }
}
