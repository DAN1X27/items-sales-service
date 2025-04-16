package danix.app.users_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoDTO {

	private Long id;

	private String username;

	private String email;

	private Double grade;

	private String country;

	private String city;

}
