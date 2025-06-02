package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseUsersChatDTO;

import java.util.List;

public interface UsersChatsService extends ChatsService {

    List<ResponseUsersChatDTO> getUserChats();

    DataDTO<Long> create(long userId, String token);
}
