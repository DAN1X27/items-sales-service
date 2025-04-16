package danix.app.chats_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {

	@NotBlank(message = "Message must not be empty")
	private String message;

}