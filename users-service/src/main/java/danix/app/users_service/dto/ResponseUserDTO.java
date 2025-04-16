package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseUserDTO {

	private Long id;

	private String username;

	private Double grade;

	private String country;

	private String city;

	@JsonProperty("is_blocked")
	private Boolean isBlocked;

	@JsonProperty("is_current_user_blocked")
	private Boolean isCurrentUserBlocked;

}
