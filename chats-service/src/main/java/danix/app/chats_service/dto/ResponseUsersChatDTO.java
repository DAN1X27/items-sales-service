package danix.app.chats_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseUsersChatDTO {

	private long id;

	@JsonProperty("user_id")
	private long userId;

}
