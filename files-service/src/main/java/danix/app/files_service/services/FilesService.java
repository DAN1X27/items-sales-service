package danix.app.files_service.services;

import danix.app.files_service.dto.ResponseFileDTO;
import danix.app.files_service.util.FileException;
import danix.app.files_service.util.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FilesService {
    private final String USERS_AVATARS;
    private final String CHATS_IMAGES;
    private final String CHATS_VIDEOS;
    private final String ANNOUNCEMENTS_IMAGES;
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesService.class);

    public FilesService(@Value("${paths.users-avatars}") String usersAvatars, @Value("${paths.chats_images_path}") String chatsImages,
                        @Value("${paths.chats_videos_path}") String chatsVideos,
                        @Value("${paths.announcements_images_path}") String announcementsImages) {
        USERS_AVATARS = usersAvatars;
        CHATS_IMAGES = chatsImages;
        CHATS_VIDEOS = chatsVideos;
        ANNOUNCEMENTS_IMAGES = announcementsImages;
    }

    public void upload(FileType type, MultipartFile file, String fileName) {
        String path = getFilePath(type);
        File newFile = new File(path, fileName);
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }
        try(FileOutputStream outputStream = new FileOutputStream(newFile)) {
            outputStream.write(file.getBytes());
        } catch (IOException e) {
            LOGGER.error("Error upload file - {}", fileName, e);
            throw new FileException("Error upload file");
        }
    }

    public ResponseFileDTO download(FileType type, String fileName) {
        Path path = Path.of(getFilePath(type), fileName);
        if (!Files.exists(path)) {
            throw new FileException("File not found");
        }
        try {
            byte[] data = Files.readAllBytes(path);
            String mediaType = switch (type) {
                case USER_AVATAR, CHAT_IMAGE, ANNOUNCEMENT_IMAGE -> "image/jpeg";
                case CHAT_VIDEO -> "video/mp4";
            };
            return new ResponseFileDTO(data, mediaType);
        } catch (IOException e) {
            LOGGER.error("Error download file - {}", fileName, e);
            throw new FileException("Error download file");
        }
    }

    public void delete(FileType type, String fileName) {
        Path path = Path.of(getFilePath(type), fileName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.error("Error delete file - {}", fileName, e);
            throw new FileException("Error delete file");
        }
    }

    private String getFilePath(FileType type) {
        return switch (type) {
            case USER_AVATAR -> USERS_AVATARS;
            case CHAT_IMAGE -> CHATS_IMAGES;
            case CHAT_VIDEO -> CHATS_VIDEOS;
            case ANNOUNCEMENT_IMAGE -> ANNOUNCEMENTS_IMAGES;
        };
    }

    @KafkaListener(topics = "deleted_chat", groupId = "groupId", containerFactory = "listFactory")
    public void deleteChatImages(List<String> images) {
        images.forEach(image -> delete(FileType.CHAT_IMAGE, image));
    }

    @KafkaListener(topics = "deleted_announcements_images", groupId = "groupId", containerFactory = "listFactory")
    public void deleteUserAnnouncementsImages(List<String> images) {
        images.forEach(image -> delete(FileType.ANNOUNCEMENT_IMAGE, image));
    }
}