package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

	@NotBlank(message = "Email must not be empty")
	private String email;

	@NotBlank(message = "Password must not be empty")
	private String password;

}
