package danix.app.authentication_service.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

	private Long id;

	private String email;

	private String password;

	private String role;

	private String city;

	private String country;

}
