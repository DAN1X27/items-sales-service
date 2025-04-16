package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.models.SupportChat;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatMapper {

    List<ResponseSupportChatDTO> toResponseSupportChatDTOList(List<SupportChat> chats);

}
