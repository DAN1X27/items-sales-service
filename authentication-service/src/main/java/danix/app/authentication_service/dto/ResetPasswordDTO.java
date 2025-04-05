package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDTO implements EmailKey {

    @NotBlank(message = "Email must not be empty")
    private String email;

    @NotBlank(message = "Password must not be empty")
    private String password;

    @NotNull(message = "Key must not be empty")
    private Integer key;

    @Override
    public String email() {
        return email;
    }

    @Override
    public int key() {
        return key;
    }
}
