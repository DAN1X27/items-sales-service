package danix.app.authentication_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResponseUserAuthenticationDTO {

	private Long id;

	private String email;

	private String role;

	private String city;

	private String country;

}
