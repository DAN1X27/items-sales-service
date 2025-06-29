package danix.app.authentication_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.authentication_service.util.StartsWithCapitalLater;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationDTO {

	@NotBlank(message = "Email must not be empty")
	@Email(message = "Email must be correct")
	private String email;

	@NotBlank(message = "Username must not be empty")
	@Size(max = 20, message = "Username cannot be more than 20 characters")
	private String username;

	@NotBlank(message = "Password must not be empty")
	private String password;

	@NotBlank(message = "Country must not be empty")
	private String country;

	@NotBlank(message = "City must not be empty")
	private String city;

	@JsonProperty("first_name")
	@NotBlank(message = "First name must not be empty")
	@StartsWithCapitalLater(message = "First name must starts with a capital letter")
	private String firstName;

	@JsonProperty("last_name")
	@NotBlank(message = "Last name must not be empty")
	@StartsWithCapitalLater(message = "Last name must starts with a capital letter")
	private String lastName;

}
