package danix.app.users_service.mapper;

import danix.app.users_service.dto.AuthenticationDTO;
import danix.app.users_service.dto.ResponseUserDTO;
import danix.app.users_service.dto.UserInfoDTO;
import danix.app.users_service.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

	ResponseUserDTO toResponseUserDTO(User user);

	UserInfoDTO toUserInfoDTO(User user);

	AuthenticationDTO toAuthenticationDTO(User user);

}
