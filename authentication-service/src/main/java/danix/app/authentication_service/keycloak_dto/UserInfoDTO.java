package danix.app.authentication_service.keycloak_dto;

import lombok.Data;

@Data
public class UserInfoDTO {

    private String id;

    private String username;

    private String email;

    private UserAttributesDTO attributes;

    private boolean enabled;

}
