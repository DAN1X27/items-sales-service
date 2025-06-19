package danix.app.chats_service.util;

import lombok.Getter;

@Getter
public enum ErrorCode {

    REQUEST_ERROR(100),

    PROCESSING_ERROR(101);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
