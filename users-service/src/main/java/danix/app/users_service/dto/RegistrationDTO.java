package danix.app.users_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationDTO {
    private String email;
    private String username;
    private String password;
    private String country;
    private String city;
}
