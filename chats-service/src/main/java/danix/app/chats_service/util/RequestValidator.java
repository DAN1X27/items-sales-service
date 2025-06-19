package danix.app.chats_service.util;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

public final class RequestValidator {

    private RequestValidator() {
    }

    public static void handleRequestErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            LocalDateTime timestamp = LocalDateTime.now();
            Map<String, ErrorData> error = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, fieldError -> new ErrorData(
                            fieldError.getDefaultMessage(), timestamp)));
            throw new RequestException(error);
        }
    }

}
