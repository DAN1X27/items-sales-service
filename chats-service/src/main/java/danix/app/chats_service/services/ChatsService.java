package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.util.ContentType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatsService {

    List<ResponseMessageDTO> getChatMessages(long id, int page, int count);

    DataDTO<Long> sendTextMessage(long chatId, String text);

    DataDTO<Long> sendFile(long chatId, MultipartFile file, ContentType contentType);

    ResponseEntity<?> getFile(long id, ContentType contentType);

    void updateMessage(long id, String text);

    void deleteMessage(long id);

    void delete(long id);

}
