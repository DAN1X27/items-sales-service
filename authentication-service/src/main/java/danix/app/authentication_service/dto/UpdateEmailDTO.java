package danix.app.authentication_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmailDTO {

	@NotBlank(message = "New email must not be empty")
	@Email(message = "New email must be correct")
	private String email;

	@NotBlank(message = "Password must not be empty")
	private String password;

}
