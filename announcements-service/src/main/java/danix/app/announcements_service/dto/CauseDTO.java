package danix.app.announcements_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CauseDTO {

    @NotBlank(message = "Cause must not be empty")
    private String cause;

}
