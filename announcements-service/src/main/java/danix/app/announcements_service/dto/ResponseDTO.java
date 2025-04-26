package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
public class ResponseDTO {

	private Long id;

	private String title;

	private Double price;

	@JsonProperty("image_id")
	private Long imageId;

	@JsonProperty("watches")
	int watchesCount;

	@JsonProperty("likes")
	int likesCount;

}
