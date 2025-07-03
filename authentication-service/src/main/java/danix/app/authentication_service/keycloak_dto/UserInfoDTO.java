package danix.app.authentication_service.keycloak_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDTO {

    private String id;

    private String username;

    private String email;

    private UserAttributesDTO attributes;

    private boolean enabled;

}
