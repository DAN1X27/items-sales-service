package danix.app.announcements_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataDTO<T> {

    private T data;

}
