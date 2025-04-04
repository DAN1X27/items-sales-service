package danix.app.users_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class UserInfoDTO {
    private Long id;
    private String username;
    private String email;
    private Double grade;
    private String country;
    private String city;
    private List<ResponseCommentDTO> comments;
}
