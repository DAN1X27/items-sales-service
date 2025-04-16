package danix.app.announcements_service.dto;

import danix.app.announcements_service.util.SortType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class SortDTO {

	@NotNull(message = "Sort type must not be empty")
	private SortType type;

	@NotNull(message = "Sort direction must not be empty")
	private Sort.Direction direction;

}
