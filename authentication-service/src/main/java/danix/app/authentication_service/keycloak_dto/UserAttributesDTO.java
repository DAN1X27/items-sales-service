package danix.app.authentication_service.keycloak_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAttributesDTO {

    @JsonProperty("user_id")
    private List<Long> id;

    @JsonProperty("first_name")
    private List<String> firstName;

    @JsonProperty("last_name")
    private List<String> lastName;

    private List<String> country;

    private List<String> city;

}
