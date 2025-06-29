package danix.app.authentication_service.mapper;

import danix.app.authentication_service.dto.RegistrationDTO;
import danix.app.authentication_service.dto.TempRegistrationDTO;
import danix.app.authentication_service.keycloak_dto.UserAttributesDTO;
import danix.app.authentication_service.keycloak_dto.CredentialsDTO;
import danix.app.authentication_service.keycloak_dto.KeycloakRegistrationDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegistrationMapper {

    TempRegistrationDTO toTempRegistrationDTO(RegistrationDTO registrationDTO);

    @Mappings({
            @Mapping(source = "firstName", target = "firstName", qualifiedByName = "stringToList"),
            @Mapping(source = "lastName", target = "lastName", qualifiedByName = "stringToList"),
            @Mapping(source = "country", target = "country", qualifiedByName = "stringToList"),
            @Mapping(source = "city", target = "city", qualifiedByName = "stringToList")
    })
    UserAttributesDTO toAttributes(RegistrationDTO registrationDTO);

    KeycloakRegistrationDTO toRegistration(RegistrationDTO registrationDTO, UserAttributesDTO attributes,
                                           List<CredentialsDTO> credentials);

    @Named("stringToList")
    default List<String> stringToList(String data) {
        return List.of(data);
    }

}
