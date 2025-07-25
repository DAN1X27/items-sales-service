package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseUsersChatDTO;

import java.util.List;

public interface UsersChatsService extends ChatsService {

    List<ResponseUsersChatDTO> getUserChats(int page, int count);

    DataDTO<Long> create(long userId);
}
