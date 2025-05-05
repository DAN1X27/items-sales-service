package danix.app.chats_service.security;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

	private Long id;

	private String email;

	private String role;

	private String city;

	private String country;

}
