package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
public class ResetPasswordDTO implements EmailKey {

	@NotBlank(message = "Email must not be empty")
	private String email;

	@Getter
    @NotBlank(message = "Password must not be empty")
	private String password;

	@NotNull(message = "Key must not be empty")
	private Integer key;

	@Override
	public String getEmail() {
		return this.email;
	}

	@Override
	public int getKey() {
		return this.key;
	}

}
