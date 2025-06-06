package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.announcements_service.util.CurrencyCode;
import danix.app.announcements_service.util.NullOrNotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class UpdateAnnouncementDTO {

	@NullOrNotBlank(message = "Title must not be empty")
	private String title;

	@NullOrNotBlank(message = "Description must not be empty")
	private String description;

	private Double price;

	private CurrencyCode currency;

	@NullOrNotBlank(message = "Phone number must not be empty")
	@JsonProperty("phone_number")
	private String phoneNumber;

	@NullOrNotBlank(message = "Country must not be empty")
	private String country;

	@NullOrNotBlank(message = "City must not be empty")
	private String city;

	@NullOrNotBlank(message = "Type must not be empty")
	private String type;

}
