package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResponseReportDTO {
    private Integer id;
    private String cause;
    @JsonProperty("user_id")
    private Long userId;
}
