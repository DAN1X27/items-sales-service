package danix.app.files_service.kafka_listeners;

import danix.app.files_service.services.FilesService;
import danix.app.files_service.util.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeletedAnnouncementListener {

    private final FilesService filesService;

    @KafkaListener(topics = "${kafka-topics.deleted_announcement}", containerFactory = "listFactory")
    public void deleteAnnouncementImages(List<String> images) {
        log.info("Deleting announcement images, size: {}", images.size());
        images.forEach(image -> filesService.delete(FileType.ANNOUNCEMENT_IMAGE, image));
    }
}
