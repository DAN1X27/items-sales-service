package danix.app.files_service.services.impl;

import danix.app.files_service.config.AppProperties;
import danix.app.files_service.services.FilesService;
import danix.app.files_service.util.FileException;
import danix.app.files_service.util.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilesServiceImpl implements FilesService {

	private final AppProperties properties;

	@Override
	public void upload(FileType type, MultipartFile file, String fileName) {
		String name = file.getOriginalFilename();
		assert name != null;
		switch (type) {
			case ANNOUNCEMENT_IMAGE, CHAT_IMAGE, USER_AVATAR -> {
				if (!name.endsWith(".jpg") && !name.endsWith(".png") && !name.endsWith(".webp")) {
					throw new FileException("Invalid file type");
				}
			}
			case CHAT_VIDEO -> {
				if (!name.endsWith(".mp4")) {
					throw new FileException("Invalid file type");
				}
			}
		}
		String path = getDirPath(type);
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

	@Override
	public ResponseEntity<?> download(FileType type, String fileName) {
		Path path = Path.of(getDirPath(type), fileName);
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

	@Override
	public void delete(FileType type, String fileName) {
		Path path = Path.of(getDirPath(type), fileName);
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException e) {
			log.error("Error delete file - {} : {}", fileName, e.getMessage(), e);
			throw new FileException("Error delete file");
		}
	}

	private String getDirPath(FileType type) {
		return switch (type) {
			case USER_AVATAR -> properties.getUsersAvatars();
			case CHAT_IMAGE -> properties.getChatsImages();
			case CHAT_VIDEO -> properties.getChatsVideos();
			case ANNOUNCEMENT_IMAGE -> properties.getAnnouncementsImages();
		};
	}

}