package danix.app.chats_service.services.impl;

import danix.app.chats_service.feign.FilesAPI;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.util.SecurityUtil;
import danix.app.chats_service.models.User;
import danix.app.chats_service.services.MessagesService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagesServiceImpl implements MessagesService {

	private final FilesAPI filesAPI;

	private final SimpMessagingTemplate messagingTemplate;

	private final SecurityUtil securityUtil;

	private final KafkaTemplate<String, List<String>> kafkaTemplate;

	@Value("${access_key}")
	private String accessKey;

	@Value("${kafka-topics.deleted-chat}")
	private String deletedChatTopic;

	@Override
	public ResponseEntity<?> getFile(Message message, ContentType contentType) {
		MediaType mediaType;
		byte[] data = switch (contentType) {
			case IMAGE -> {
				if (message.getContentType() != ContentType.IMAGE) {
					throw new ChatException("Message is not image");
				}
				mediaType = MediaType.IMAGE_JPEG;
				yield filesAPI.downloadImage(message.getText(), accessKey);
			}
			case VIDEO -> {
				if (message.getContentType() != ContentType.VIDEO) {
					throw new ChatException("Message is not video");
				}
				mediaType = MediaType.parseMediaType("video/mp4");
				yield filesAPI.downloadVideo(message.getText(), accessKey);
			}
			default -> throw new IllegalArgumentException("Invalid content type");
		};
		return ResponseEntity.status(HttpStatus.OK)
				.contentType(mediaType)
				.body(data);
	}

	@Override
	public void saveFile(MultipartFile file, Message message, ContentType contentType, Runnable delete) {
		try {
			switch (contentType) {
				case IMAGE -> filesAPI.saveImage(file, message.getText(), accessKey);
				case VIDEO -> filesAPI.saveVideo(file, message.getText(), accessKey);
				default -> throw new IllegalArgumentException("Invalid content type");
			}
		}
		catch (Exception e) {
			delete.run();
			throw e;
		}
	}

	@Override
	public void updateMessage(Message message, String text, String topic) {
		User user = securityUtil.getCurrentUser();
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

	@Override
	public void deleteMessage(Message message, String topic, Runnable delete) {
		User user = securityUtil.getCurrentUser();
		if (message.getSenderId() != user.getId()) {
			throw new ChatException("You are not owner of this message");
		}
		delete.run();
		switch (message.getContentType()) {
			case IMAGE -> filesAPI.deleteImage(message.getText(), accessKey);
			case VIDEO -> filesAPI.deleteVideo(message.getText(), accessKey);
		}
		messagingTemplate.convertAndSend(topic, Map.of("deleted_message", message.getId()));
	}

	@Override
	@Async("virtualExecutor")
	public void deleteFiles(Function<Integer, List<? extends Message>> getFiles, Runnable deleteChat) {
		int page = 0;
		List<String> files;
		do {
			files = getFiles.apply(page).stream()
					.map(Message::getText)
					.toList();
			if (!files.isEmpty()) {
				kafkaTemplate.send(deletedChatTopic, files);
				page++;
			}
		}
		while (!files.isEmpty());
		deleteChat.run();
	}

	@Override
	public String getFileName(ContentType contentType) {
		return switch (contentType) {
			case IMAGE -> UUID.randomUUID() + ".jpg";
			case VIDEO -> UUID.randomUUID() + ".mp4";
			default -> throw new IllegalArgumentException("Invalid content type");
		};
	}
}
