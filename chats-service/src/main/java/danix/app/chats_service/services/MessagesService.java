package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.feign.FilesService;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.security.User;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static danix.app.chats_service.security.UserDetailsServiceImpl.getCurrentUser;

@Service
@RequiredArgsConstructor
public class MessagesService {

	private final FilesService filesService;

	private final SimpMessagingTemplate messagingTemplate;

	private final KafkaTemplate<String, List<String>> kafkaTemplate;

	@Value("${access_key}")
	private String accessKey;

	public ResponseEntity<?> getFile(Message message, ContentType contentType) {
		MediaType mediaType;
		byte[] data = switch (contentType) {
			case IMAGE -> {
				if (message.getContentType() != ContentType.IMAGE) {
					throw new ChatException("Message is not image");
				}
				mediaType = MediaType.IMAGE_JPEG;
				yield filesService.downloadImage(message.getText(), accessKey);
			}
			case VIDEO -> {
				if (message.getContentType() != ContentType.VIDEO) {
					throw new ChatException("Message is not video");
				}
				mediaType = MediaType.parseMediaType("video/mp4");
				yield filesService.downloadVideo(message.getText(), accessKey);
			}
			default -> throw new IllegalArgumentException("Invalid content type");
		};
		return ResponseEntity.status(HttpStatus.OK)
				.contentType(mediaType)
				.body(data);
	}

	public DataDTO<Long> saveFile(MultipartFile file, Message message, ContentType contentType, Runnable delete) {
		try {
			switch (contentType) {
				case IMAGE -> filesService.saveImage(file, message.getText(), accessKey);
				case VIDEO -> filesService.saveVideo(file, message.getText(), accessKey);
				default -> throw new IllegalArgumentException("Invalid content type");
			}
		}
		catch (Exception e) {
			delete.run();
			throw e;
		}
		return new DataDTO<>(message.getId());
	}

	public void updateMessage(Message message, String text, String topic) {
		User user = getCurrentUser();
		if (message.getSenderId() != user.getId()) {
			throw new ChatException("You are not owner of this message");
		}
		switch (message.getContentType()) {
			case IMAGE -> throw new ChatException("Image cannot be updated");
			case VIDEO -> throw new ChatException("Video cannot be updated");
		}
		message.setText(text);
		Map<String, Object> response = Map.of("updated_message", message.getId(), "text", text);
		messagingTemplate.convertAndSend(topic, response);
	}

	public void deleteMessage(Message message, String topic, Runnable delete) {
		User user = getCurrentUser();
		if (message.getSenderId() != user.getId()) {
			throw new ChatException("You are not owner of this message");
		}
		delete.run();
		switch (message.getContentType()) {
			case IMAGE -> filesService.deleteImage(message.getText(), accessKey);
			case VIDEO -> filesService.deleteVideo(message.getText(), accessKey);
		}
		messagingTemplate.convertAndSend(topic, Map.of("deleted_message", message.getId()));
	}

	public void deleteFiles(Function<Integer, List<? extends Message>> getFiles, Runnable deleteChat) {
		Thread.startVirtualThread(() -> {
			int page = 0;
			List<String> files;
			do {
				files = getFiles.apply(page).stream()
						.map(Message::getText)
						.toList();
				if (!files.isEmpty()) {
					kafkaTemplate.send("deleted_chat", files);
					page++;
				}
			}
			while (!files.isEmpty());
			deleteChat.run();
		});
	}

	public static String getFileName(ContentType contentType) {
		return switch (contentType) {
			case IMAGE -> UUID.randomUUID() + ".jpg";
			case VIDEO -> UUID.randomUUID() + ".mp4";
			default -> throw new IllegalArgumentException("Invalid content type");
		};
	}

}
