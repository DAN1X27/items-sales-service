package danix.app.users_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

@Getter
@Setter
@Builder
public class ResponseAvatarDTO {
    private byte[] data;
    private MediaType mediaType;
}
