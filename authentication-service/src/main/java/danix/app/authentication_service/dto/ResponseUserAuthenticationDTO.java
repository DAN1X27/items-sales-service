package danix.app.authentication_service.dto;

import lombok.Data;

@Data
public class ResponseUserAuthenticationDTO {

	private Long id;

	private String email;

	private String role;

	private String city;

	private String country;

}
