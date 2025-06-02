package danix.app.chats_service.services;

import danix.app.chats_service.models.Message;
import danix.app.chats_service.util.ContentType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Function;

public interface MessagesService {

    ResponseEntity<?> getFile(Message message, ContentType contentType);

    void saveFile(MultipartFile file, Message message, ContentType contentType, Runnable delete);

    void updateMessage(Message message, String text, String topic);

    void deleteMessage(Message message, String topic, Runnable delete);

    void deleteFiles(Function<Integer, List<? extends Message>> getFiles, Runnable deleteChat);

    String getFileName(ContentType contentType);
}
