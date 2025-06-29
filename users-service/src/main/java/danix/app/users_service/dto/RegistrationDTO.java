package danix.app.users_service.dto;

import lombok.Data;

@Data
public class RegistrationDTO {

	private String email;

	private String username;

	private String country;

	private String city;

	private String firstName;

	private String lastName;

}
