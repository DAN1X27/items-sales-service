package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
public class ResponseAnnouncementDTO {

	private Long id;

	private String title;

	private Double price;

	private String country;

	private String city;

	private String type;

	@JsonProperty("owner_id")
	private Long ownerId;

	@JsonProperty("image_id")
	private Long imageId;

	@JsonProperty("watches")
	int watchesCount;

	@JsonProperty("likes")
	int likesCount;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

}
