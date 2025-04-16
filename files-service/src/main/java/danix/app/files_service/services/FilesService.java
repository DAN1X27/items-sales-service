package danix.app.files_service.services;

import danix.app.files_service.util.FileException;
import danix.app.files_service.util.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@Slf4j
public class FilesService {

	private final String USERS_AVATARS_PATH;

	private final String CHATS_IMAGES_PATH;

	private final String CHATS_VIDEOS_PATH;

	private final String ANNOUNCEMENTS_IMAGES_PATH;

	public FilesService(@Value("${paths.users-avatars}") String usersAvatarsPath,
                        @Value("${paths.chats_images}") String chatsImagesPath,
                        @Value("${paths.chats_videos}") String chatsVideosPath,
                        @Value("${paths.announcements_images}") String announcementsImagesPath) {
		USERS_AVATARS_PATH = usersAvatarsPath;
		CHATS_IMAGES_PATH = chatsImagesPath;
		CHATS_VIDEOS_PATH = chatsVideosPath;
		ANNOUNCEMENTS_IMAGES_PATH = announcementsImagesPath;
	}

	public void upload(FileType type, MultipartFile file, String fileName) {
		assert file.getOriginalFilename() != null;
		switch (type) {
			case ANNOUNCEMENT_IMAGE, CHAT_IMAGE, USER_AVATAR -> {
				if (file.getOriginalFilename().endsWith(".jpg")
					&& !file.getOriginalFilename().endsWith(".png")
					&& !file.getOriginalFilename().endsWith(".webp")) {
					throw new FileException("Invalid file type");
				}
			}
			case CHAT_VIDEO -> {
				if (file.getOriginalFilename().endsWith(".mp4")) {
					throw new FileException("Invalid file type");
				}
			}
		}
		String path = getFilePath(type);
		File newFile = new File(path, fileName);
		if (!newFile.getParentFile().exists()) {
			newFile.getParentFile().mkdirs();
		}
		try (FileOutputStream outputStream = new FileOutputStream(newFile)) {
			outputStream.write(file.getBytes());
		}
		catch (IOException e) {
			log.error("Error upload file - {} : {}", fileName, e.getMessage(), e);
			throw new FileException("Error upload file");
		}
	}

	public ResponseEntity<?> download(FileType type, String fileName) {
		Path path = Path.of(getFilePath(type), fileName);
		if (!Files.exists(path)) {
			throw new FileException("File not found");
		}
		try {
			byte[] data = Files.readAllBytes(path);
			MediaType mediaType = type == FileType.CHAT_VIDEO
					? MediaType.parseMediaType("video/mp4") : MediaType.IMAGE_JPEG;
			return ResponseEntity.status(HttpStatus.OK)
                    .contentType(mediaType)
                    .body(data);
		}
		catch (IOException e) {
			log.error("Error download file - {} : {}", fileName, e.getMessage(), e);
			throw new FileException("Error download file");
		}
	}

	public void delete(FileType type, String fileName) {
		Path path = Path.of(getFilePath(type), fileName);
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException e) {
			log.error("Error delete file - {} : {}", fileName, e.getMessage(), e);
			throw new FileException("Error delete file");
		}
	}

	private String getFilePath(FileType type) {
		return switch (type) {
			case USER_AVATAR -> USERS_AVATARS_PATH;
			case CHAT_IMAGE -> CHATS_IMAGES_PATH;
			case CHAT_VIDEO -> CHATS_VIDEOS_PATH;
			case ANNOUNCEMENT_IMAGE -> ANNOUNCEMENTS_IMAGES_PATH;
		};
	}

	@KafkaListener(topics = "deleted_chat", containerFactory = "listFactory")
	public void deleteChatImages(List<String> files) {
		files.forEach(file -> {
			if (file.endsWith(".jpg")) {
				delete(FileType.CHAT_IMAGE, file);
			}
			else if (file.endsWith(".mp4")) {
				delete(FileType.CHAT_VIDEO, file);
			}
		});
	}

	@KafkaListener(topics = "deleted_announcements_images", containerFactory = "listFactory")
	public void deleteUserAnnouncementsImages(List<String> images) {
		images.forEach(image -> delete(FileType.ANNOUNCEMENT_IMAGE, image));
	}

}