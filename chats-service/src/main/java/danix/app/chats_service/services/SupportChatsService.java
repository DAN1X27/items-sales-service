package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface SupportChatsService extends ChatsService {

    List<ResponseSupportChatDTO> getUserChats(int page, int count);

    List<ResponseSupportChatDTO> findAll(int page, int count, Sort.Direction sortDirection);

    List<ResponseMessageDTO> getChatMessages(long id, int page, int count);

    DataDTO<Long> create(String message);

    void close(long id);

    void setStatusToWait(long id);

    void setStatusToProcessing(long id);

}
