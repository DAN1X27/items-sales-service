package danix.app.files_service.kafka_listeners;

import danix.app.files_service.services.FilesService;
import danix.app.files_service.util.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeletedChatListener {

    private final FilesService filesService;

    @KafkaListener(topics = "deleted_chat", containerFactory = "listFactory")
    public void deleteChatFiles(List<String> files) {
        for (String file : files) {
            if (file.endsWith(".jpg")) {
                filesService.delete(FileType.CHAT_IMAGE, file);
            }
            else {
               filesService.delete(FileType.CHAT_VIDEO, file);
            }
        }
    }

}
