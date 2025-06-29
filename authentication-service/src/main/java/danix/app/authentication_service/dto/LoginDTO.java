package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {

	@NotBlank(message = "Username must not be empty")
	private String username;

	@NotBlank(message = "Password must not be empty")
	private String password;

}
