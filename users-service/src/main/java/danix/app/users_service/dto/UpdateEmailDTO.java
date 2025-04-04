package danix.app.users_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmailDTO {
    @Email(message = "Email must be correct")
    private String email;
    @NotEmpty(message = "Password must not be empty")
    private String password;
}
