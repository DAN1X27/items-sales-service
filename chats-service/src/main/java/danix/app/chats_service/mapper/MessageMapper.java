package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.util.ContentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessageMapper {

    @Mapping(target = "text", source = "message", qualifiedByName = "text")
    ResponseMessageDTO toResponseMessageDTO(Message message);

    List<ResponseMessageDTO> toResponseMessageDTOList(List<Message> messages);

    @Named(value = "text")
    default String text(Message message) {
        return message.getContentType() == ContentType.TEXT ? message.getText() : null;
    }
}