package danix.app.authentication_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TempRegistrationDTO {

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String city;

    private String country;

}
