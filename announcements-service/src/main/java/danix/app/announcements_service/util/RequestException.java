package danix.app.announcements_service.util;

import lombok.Getter;

import java.util.Map;

@Getter
public class RequestException extends RuntimeException {

    private final Map<String, ErrorData> error;

    public RequestException(Map<String, ErrorData> error) {
        super();
        this.error = error;
    }
}
