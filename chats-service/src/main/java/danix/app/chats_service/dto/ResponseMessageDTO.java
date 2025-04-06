package danix.app.chats_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import danix.app.chats_service.util.ContentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseMessageDTO {
    private long id;
    @JsonProperty("content_type")
    private ContentType contentType;
    private String text;
    @JsonProperty("sender_id")
    private long senderId;
    @JsonProperty("sent_time")
    private LocalDateTime sentTime;
}