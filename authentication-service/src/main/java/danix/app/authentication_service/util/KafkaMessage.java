package danix.app.authentication_service.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KafkaMessage {
    private String email;
    private String message;
}
