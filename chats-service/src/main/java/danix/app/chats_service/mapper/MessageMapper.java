package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.models.*;
import danix.app.chats_service.util.ContentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface MessageMapper {

    @Mapping(target = "sentTime",  expression = "java(LocalDateTime.now())")
    @Mapping(target = "chat", source = "chat")
    @Mapping(target = "id", ignore = true)
    UsersChatMessage toUsersChatMessage(String text, Long senderId, UsersChat chat, ContentType contentType);

    @Mapping(target = "sentTime",  expression = "java(LocalDateTime.now())")
    @Mapping(target = "chat", source = "chat")
    @Mapping(target = "id", ignore = true)
    SupportChatMessage toSupportChatMessage(String text, Long senderId, SupportChat chat, ContentType contentType);

    @Mapping(target = "text", source = "message", qualifiedByName = "text")
    ResponseMessageDTO toResponseMessageDTO(Message message);

    List<ResponseMessageDTO> toResponseMessageDTOList(List<Message> messages);

    @Named(value = "text")
    default String text(Message message) {
        return message.getContentType() == ContentType.TEXT ? message.getText() : null;
    }
}