package danix.app.chats_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.chats_service.models.SupportChat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseSupportChatDTO{
    private long id;
    private SupportChat.Status status;
    @JsonProperty("user_id")
    private long userId;
}
