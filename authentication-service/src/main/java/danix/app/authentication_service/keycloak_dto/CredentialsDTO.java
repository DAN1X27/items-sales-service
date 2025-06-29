package danix.app.authentication_service.keycloak_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CredentialsDTO {

    private String type;

    private String value;

    private boolean temporary;

}
