package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class EmailKeyDTO implements EmailKey {

	@NotEmpty(message = "Email must not be empty")
	private String email;

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
