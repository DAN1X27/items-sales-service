package danix.app.users_service.dto;

import danix.app.users_service.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationDTO {

	private long id;

	private String email;

	private String password;

	private User.Role role;

	private String city;

	private String country;

}
