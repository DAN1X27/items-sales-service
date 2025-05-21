package danix.app.authentication_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface EmailKeyDTO {

	@NotBlank(message = "Email must not be empty")
	String getEmail();

	@NotNull(message = "Key is required")
	Integer getKey();

}
