package danix.app.users_service.mapper;

import danix.app.users_service.dto.ResponseReportDTO;
import danix.app.users_service.models.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReportMapper {

	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "senderId", source = "sender.id")
	ResponseReportDTO toResponseDTO(Report report);

	List<ResponseReportDTO> toResponseDTOList(List<Report> report);

}
