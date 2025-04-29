package danix.app.announcements_service.dto;

import lombok.Data;

@Data
public class ShowReportDTO {

    private Long id;

    private String cause;

    private Long senderId;

    private ResponseAnnouncementDTO announcement;
}
