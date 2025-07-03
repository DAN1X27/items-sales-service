package danix.app.authentication_service.dto;

import lombok.Data;

@Data
public class UpdateEmailKeyDTO implements EmailKeyDTO {

	private String email;

	private Integer key;
}
