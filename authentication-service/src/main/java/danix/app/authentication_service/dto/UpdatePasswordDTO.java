package danix.app.authentication_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdatePasswordDTO {

    @JsonProperty("old_password")
    @NotEmpty(message = "Old password is required")
    private String oldPassword;

    @JsonProperty("new_password")
    @NotBlank(message = "New password must not be empty")
    private String newPassword;

}
