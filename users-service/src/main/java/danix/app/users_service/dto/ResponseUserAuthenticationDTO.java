package danix.app.users_service.dto;

import danix.app.users_service.models.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResponseUserAuthenticationDTO {
    private Long id;
    private String email;
    private String password;
    private User.Role role;
    private String city;
    private String country;
}
