package danix.app.announcements_service.mapper;

import danix.app.announcements_service.dto.ResponseReportDTO;
import danix.app.announcements_service.dto.ShowReportDTO;
import danix.app.announcements_service.models.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReportMapper {

    ResponseReportDTO toResponseDTO(Report report);

    List<ResponseReportDTO> toResponseDTOList(List<Report> reports);

    @Mapping(target = "announcement", ignore = true)
    ShowReportDTO toShowDTO(Report report);
}
