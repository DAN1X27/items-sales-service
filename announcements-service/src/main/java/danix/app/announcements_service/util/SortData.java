package danix.app.announcements_service.util;


import org.springframework.data.domain.Sort;

public record SortData(SortType type, Sort.Direction direction) {
}
