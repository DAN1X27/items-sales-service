package danix.app.announcements_service.security;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class User {

	private Long id;

	private String email;

	private String role;

	private String city;

	private String country;

}
