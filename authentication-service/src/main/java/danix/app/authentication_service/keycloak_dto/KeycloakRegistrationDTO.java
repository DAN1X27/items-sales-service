package danix.app.authentication_service.keycloak_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeycloakRegistrationDTO {

    private String username;

    private String email;

    private boolean enabled;

    private boolean emailVerified;

    private List<CredentialsDTO> credentials;

    private UserAttributesDTO attributes;

}
