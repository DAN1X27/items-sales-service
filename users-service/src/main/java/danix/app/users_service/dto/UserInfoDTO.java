package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoDTO {

	private Long id;

	private String username;

	private String email;

	private Double grade;

	@JsonProperty("grades_count")
	private Integer gradesCount;

	private String country;

	private String city;

}
