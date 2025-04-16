package danix.app.announcements_service.mapper;

import danix.app.announcements_service.dto.ResponseReportDTO;
import danix.app.announcements_service.models.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ReportMapper {

    protected AnnouncementMapper announcementMapper;

    @Autowired
    private void setAnnouncementMapper(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Mapping(target = "announcement",
            expression = "java(announcementMapper.toResponseDTO(report.getAnnouncement(), currency))")
    protected abstract ResponseReportDTO toResponseDTO(Report report, String currency);

    public List<ResponseReportDTO> toResponseDTOList(List<Report> reports, String currency) {
        return reports.stream()
                .map(report -> toResponseDTO(report, currency))
                .toList();
    }

}
