package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.models.*;
import danix.app.chats_service.util.ContentType;
import danix.app.chats_service.util.EncryptUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public abstract class MessageMapper {

    @Autowired
    protected EncryptUtil encryptUtil;

    @Mapping(target = "sentTime",  expression = "java(LocalDateTime.now())")
    @Mapping(target = "chat", source = "chat")
    @Mapping(target = "id", ignore = true)
    public abstract UsersChatMessage toUsersChatMessage(String text, Long senderId, UsersChat chat, ContentType contentType);

    @Mapping(target = "sentTime",  expression = "java(LocalDateTime.now())")
    @Mapping(target = "chat", source = "chat")
    @Mapping(target = "id", ignore = true)
    public abstract SupportChatMessage toSupportChatMessage(String text, Long senderId, SupportChat chat, ContentType contentType);

    @Mapping(target = "text", source = "message", qualifiedByName = "text")
    public abstract ResponseMessageDTO toResponseMessageDTO(Message message);

    public abstract List<ResponseMessageDTO> toResponseMessageDTOList(List<Message> messages);

    @Named(value = "text")
    protected String text(Message message) {
        if (message.getContentType() == ContentType.TEXT) {
            return encryptUtil.decrypt(message.getText());
        }
        return null;
    }
}