package danix.app.chats_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class DataDTO<T> {

	private T data;

}
