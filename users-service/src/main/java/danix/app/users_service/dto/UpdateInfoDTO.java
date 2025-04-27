package danix.app.users_service.dto;

import danix.app.users_service.util.NullOrNotBlank;
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

}
