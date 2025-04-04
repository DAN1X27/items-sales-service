package danix.app.chats_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.chats_service.models.Message;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseMessageDTO {
    private long id;
    @JsonProperty("content_type")
    private Message.ContentType contentType;
    private String text;
    @JsonProperty("sender_id")
    private long senderId;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
}
