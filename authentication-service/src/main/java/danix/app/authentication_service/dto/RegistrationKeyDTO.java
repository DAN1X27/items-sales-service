package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationKeyDTO {

    @NotEmpty(message = "Email must not be empty")
    private String email;

    @NotNull(message = "Key must not be empty")
    private Integer key;
}
