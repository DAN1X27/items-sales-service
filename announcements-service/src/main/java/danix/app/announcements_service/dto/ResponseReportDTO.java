package danix.app.announcements_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ResponseReportDTO {
    private Long id;
    private ResponseDTO announcement;
    private String cause;
    @JsonProperty("sender_id")
    private Long senderId;
}
