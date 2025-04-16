package danix.app.authentication_service.mapper;

import danix.app.authentication_service.dto.ResponseUserAuthenticationDTO;
import danix.app.authentication_service.security.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    ResponseUserAuthenticationDTO toResponseDTO(User user);

}