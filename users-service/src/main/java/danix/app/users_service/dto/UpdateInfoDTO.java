package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.users_service.util.NullOrNotBlank;
import danix.app.users_service.util.NullOrStartsWithCapitalLater;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInfoDTO {

	@NullOrNotBlank(message = "Username must not be empty")
	private String username;

	@NullOrNotBlank(message = "Country must not be empty")
	private String country;

	@NullOrNotBlank(message = "City must not be empty")
	private String city;

	@JsonProperty("first_name")
	@NullOrNotBlank(message = "First name must not be empty")
	@NullOrStartsWithCapitalLater(message = "First name must starts with a capital letter")
	private String firstName;

	@JsonProperty("last_name")
	@NullOrNotBlank(message = "Last name must not be empty")
	@NullOrStartsWithCapitalLater(message = "Last name must starts with a capital letter")
	private String lastName;

}
