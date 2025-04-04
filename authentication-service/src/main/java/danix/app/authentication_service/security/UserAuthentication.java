package danix.app.authentication_service.security;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserAuthentication {
    private Long id;
    private String email;
    private String password;
    private String role;
}
