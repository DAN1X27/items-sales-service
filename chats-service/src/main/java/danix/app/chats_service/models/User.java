package danix.app.chats_service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
