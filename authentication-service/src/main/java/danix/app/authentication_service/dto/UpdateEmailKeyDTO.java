package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateEmailKeyDTO implements EmailKeyDTO {

	@NotBlank(message = "Email must not be empty")
	private String email;

	@NotNull(message = "Key is required")
	private Integer key;
}
