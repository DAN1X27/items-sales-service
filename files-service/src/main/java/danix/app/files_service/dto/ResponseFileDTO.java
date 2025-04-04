package danix.app.files_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseFileDTO {
    private byte[] data;
    private String mediaType;
}
