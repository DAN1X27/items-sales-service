package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseUserDTO {

	private Long id;

	private String username;

	private String firstName;

	private String lastName;

	private Double grade;

	@JsonProperty("grades_count")
	private Integer gradesCount;

	private String country;

	private String city;

	@JsonProperty("is_blocked")
	private Boolean isBlocked;

	@JsonProperty("is_current_user_blocked")
	private Boolean isCurrentUserBlocked;

}
