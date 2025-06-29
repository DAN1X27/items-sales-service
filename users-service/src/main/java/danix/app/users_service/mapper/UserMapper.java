package danix.app.users_service.mapper;

import danix.app.users_service.dto.RegistrationDTO;
import danix.app.users_service.dto.ResponseUserDTO;
import danix.app.users_service.dto.UserInfoDTO;
import danix.app.users_service.models.TempUser;
import danix.app.users_service.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

	@Mapping(target = "gradesCount", expression = "java(user.getGrades().size())")
	ResponseUserDTO toResponseUserDTO(User user);

	@Mapping(target = "gradesCount", expression = "java(user.getGrades().size())")
	UserInfoDTO toUserInfoDTO(User user);

	TempUser toTempUserFromRegistrationDTO(RegistrationDTO registrationDTO);

	User fromTempUser(TempUser tempUser);

}
