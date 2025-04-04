package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ResponseCommentDTO {
    private Long id;
    private String text;
    @JsonProperty("sender_id")
    private Long senderId;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
