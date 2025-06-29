package danix.app.announcements_service.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

	private Long id;

	private String username;

	private String email;

	private String firstName;

	private String lastName;

	private String city;

	private String country;

}
