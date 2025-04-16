package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDTO {

	private String title;

	private String description;

	private Double price;

	private String currency;

	@JsonProperty("phone_number")
	private String phoneNumber;

	private String country;

	private String city;

	private String type;

}
