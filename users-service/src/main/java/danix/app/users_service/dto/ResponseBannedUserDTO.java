package danix.app.users_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseBannedUserDTO {

    @JsonProperty("user_id")
    private Long userId;

    private String cause;

}
