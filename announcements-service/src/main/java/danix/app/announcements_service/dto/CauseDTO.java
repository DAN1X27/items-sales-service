package danix.app.announcements_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CauseDTO {

    @NotBlank(message = "Cause must not be empty")
    private String cause;

}
