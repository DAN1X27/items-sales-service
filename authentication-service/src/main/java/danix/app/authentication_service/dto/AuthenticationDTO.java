package danix.app.authentication_service.dto;

import lombok.Data;

@Data
public class AuthenticationDTO {

    private String username;

    private String email;

    private String password;

}
