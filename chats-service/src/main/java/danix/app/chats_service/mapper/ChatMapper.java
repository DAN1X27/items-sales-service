package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseUsersChatDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.models.SupportChat;
import danix.app.chats_service.models.UsersChat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

import static danix.app.chats_service.security.UserDetailsServiceImpl.getCurrentUser;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatMapper {

    @Mapping(target = "status", expression = "java(SupportChat.Status.WAIT)")
    SupportChat toSupportChat(Long userId);

    List<ResponseSupportChatDTO> toResponseSupportChatDTOList(List<SupportChat> chats);

    UsersChat toUsersChat(Long user1Id, Long user2Id);

    @Mapping(target = "userId", source = "chat", qualifiedByName = "userId")
    ResponseUsersChatDTO toResponseUsersChatDTO(UsersChat chat);

    List<ResponseUsersChatDTO> toResponseUsersChatDTOList(List<UsersChat> chats);

    @Named("userId")
    default long userId(UsersChat chat) {
        return chat.getUser1Id() == getCurrentUser().getId() ? chat.getUser2Id() : chat.getUser1Id();
    }
}
