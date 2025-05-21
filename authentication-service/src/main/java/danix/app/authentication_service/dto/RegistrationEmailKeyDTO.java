package danix.app.authentication_service.dto;

import lombok.Data;

@Data
public class RegistrationEmailKeyDTO implements EmailKeyDTO {
    private String email;
    private Integer key;
}
