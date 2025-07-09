package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.announcements_service.util.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAnnouncementDTO {

	@NotBlank(message = "Title must not be empty")
	@Size(max = 100, message = "Title cannot be longer then 100 characters")
	private String title;

	@NullOrNotBlank(message = "Description must not be empty")
	@Size(max = 250, message = "Description cannot be longer then 250 characters")
	private String description;

	@NotNull(message = "Price must not be empty")
	private Double price;

	@NotBlank(message = "Type must not be empty")
	private String type;

	@JsonProperty("phone_number")
	@NotBlank(message = "Phone number must not be empty")
	private String phoneNumber;

	@NotBlank(message = "Country must not be empty")
	private String country;

	@NotBlank(message = "City must not be empty")
	private String city;

}
