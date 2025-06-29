package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ResetPasswordDTO implements EmailKeyDTO {

	private String email;

	@NotBlank(message = "Password must not be empty")
	private String password;

	private Integer key;
}
