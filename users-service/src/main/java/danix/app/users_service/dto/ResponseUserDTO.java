package danix.app.users_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ResponseUserDTO {
    private Long id;
    private String username;
    private Double grade;
    private String country;
    private String city;
}
