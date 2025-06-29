package danix.app.chats_service.mapper;

import danix.app.chats_service.dto.ResponseUsersChatDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.models.SupportChat;
import danix.app.chats_service.models.UsersChat;
import danix.app.chats_service.util.SecurityUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ChatMapper {

    @Autowired
    protected SecurityUtil securityUtil;

    @Mapping(target = "status", expression = "java(SupportChat.Status.WAIT)")
    public abstract SupportChat toSupportChat(Long userId);

    @Mapping(target = "userId", source = "chat", qualifiedByName = "supportChatUserId")
    public abstract ResponseSupportChatDTO toResponseSupportChatDTO(SupportChat chat);

    public abstract List<ResponseSupportChatDTO> toResponseSupportChatDTOList(List<SupportChat> chats);

    public abstract UsersChat toUsersChat(Long user1Id, Long user2Id);

    @Mapping(target = "userId", source = "chat", qualifiedByName = "usersChatUserId")
    public abstract ResponseUsersChatDTO toResponseUsersChatDTO(UsersChat chat);

    public abstract List<ResponseUsersChatDTO> toResponseUsersChatDTOList(List<UsersChat> chats);

    @Named("usersChatUserId")
    protected long userId(UsersChat chat) {
        return chat.getUser1Id() == securityUtil.getCurrentUser().getId() ? chat.getUser2Id() : chat.getUser1Id();
    }

    @Named("supportChatUserId")
    protected long userId(SupportChat chat) {
        return chat.getUserId() == securityUtil.getCurrentUser().getId() ? chat.getAdminId() : chat.getUserId();
    }
}
