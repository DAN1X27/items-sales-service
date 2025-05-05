package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShowAnnouncementDTO {

	private Long id;

	private String title;

	private String description;

	private String country;

	private String city;

	@JsonProperty("phone_number")
	private String phoneNumber;

	private String type;

	@JsonProperty("owner_id")
	private Long ownerId;

	private Double price;

	@JsonProperty("watches")
	private int watchesCount;

	@JsonProperty("likes")
	private int likesCount;

	@JsonProperty("is_liked")
	private boolean isLiked;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

	@JsonProperty("expired_date")
	private LocalDateTime expiredDate;

	@JsonProperty("images_ids")
	private List<DataDTO<Long>> imagesIds;

}
