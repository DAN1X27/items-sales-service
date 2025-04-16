package danix.app.announcements_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmailMessageDTO {
    private String email;
    private String message;
}
