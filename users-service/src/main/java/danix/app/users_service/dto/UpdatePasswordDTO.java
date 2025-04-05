package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordDTO {

    @NotBlank(message = "Old password must not be empty")
    @JsonProperty("old_password")
    private String oldPassword;

    @NotBlank(message = "New password must not be empty")
    @JsonProperty("new_password")
    private String newPassword;
}
