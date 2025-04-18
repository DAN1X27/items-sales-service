package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.feign.UsersService;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.Chat;
import danix.app.chats_service.models.ChatMessage;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.repositories.ChatsRepository;
import danix.app.chats_service.repositories.MessagesRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static danix.app.chats_service.security.UserDetailsServiceImpl.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ChatsService {

	private final ChatsRepository chatsRepository;

	private final MessagesRepository messagesRepository;

	private final UsersService usersService;

	private final SimpMessagingTemplate messagingTemplate;

	private final KafkaTemplate<String, List<String>> kafkaTemplate;

	private final MessagesService messagesService;

	private final MessageMapper messageMapper;

	public List<ResponseChatDTO> getUserChats() {
		User currentUser = getCurrentUser();
		return chatsRepository.findAllByUser1IdOrUser2Id(currentUser.getId(), currentUser.getId()).stream()
			    .map(chat -> new ResponseChatDTO(chat.getId(),
					chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id()))
			    .toList();
	}

	public List<ResponseMessageDTO> getChatMessages(long chatId, int page, int count) {
		Chat chat = chatsRepository.findById(chatId).orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		List<Message> messages = messagesRepository.findAllByChat(chat, PageRequest.of(page, count,
				Sort.by(Sort.Direction.DESC, "id")));
		return messageMapper.toResponseMessageDTOList(messages);
	}

	@Transactional
	public DataDTO<Long> create(long userId, String token) {
		User currentUser = getCurrentUser();
		if (isBlocked(userId, token)) {
			throw new ChatException("User has blocked you");
		}
		chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), userId).ifPresent(chat -> {
			throw new ChatException("Chat already exists");
		});
		Chat chat = chatsRepository.save(new Chat(currentUser.getId(), userId));
		messagingTemplate.convertAndSend("/topic/user/" + userId + "/main",
				Map.of("created_chat", chat.getId()));
		return new DataDTO<>(chat.getId());
	}

	@Transactional
	public DataDTO<Long> sendTextMessage(long chatId, String text, String token) {
		ChatMessage message = saveMessage(chatId, text, ContentType.TEXT, token);
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	@Transactional
	public DataDTO<Long> sendFile(long chatId, MultipartFile file, String token, ContentType contentType) {
		String fileName = MessagesService.getFileName(contentType);
		ChatMessage message = saveMessage(chatId, fileName, contentType, token);
		DataDTO<Long> result = messagesService.saveFile(file, message, contentType,
				() -> messagesRepository.delete(message));
		sendMessageOnTopic(message);
		return result;
	}

	public ResponseEntity<?> getFile(long id, ContentType contentType) {
		ChatMessage message = getMessageById(id);
		checkUserInChat(message.getChat());
		return messagesService.getFile(message, contentType);
	}

	private ChatMessage saveMessage(long chatId, String text, ContentType contentType, String token) {
		User currentUser = getCurrentUser();
		Chat chat = chatsRepository.findById(chatId).orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		if (isBlocked(currentUser.getId(), token)) {
			throw new ChatException("User has blocked you");
		}
		return messagesRepository.save(ChatMessage.builder()
			.chat(chat)
			.text(text)
			.senderId(currentUser.getId())
			.sentTime(LocalDateTime.now())
			.contentType(contentType)
			.build());
	}

	@Transactional
	public void updateMessage(long id, String text) {
		ChatMessage message = getMessageById(id);
		messagesService.updateMessage(message, text, "/topic/chat/" + message.getChat().getId());
	}

	@Transactional
	public void deleteMessage(long id) {
		ChatMessage message = getMessageById(id);
		String topic = "/topic/chat/" + message.getChat().getId();
		messagesService.deleteMessage(message, topic, () -> messagesRepository.delete(message));
	}

	@Transactional
	public void delete(long id) {
		Chat chat = chatsRepository.findById(id).orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		List<String> files = messagesRepository
			.findAllByChatAndContentTypeIn(chat, List.of(ContentType.IMAGE, ContentType.VIDEO)).stream()
				.map(ChatMessage::getText)
				.toList();
		kafkaTemplate.send("deleted_chat", files);
		chatsRepository.deleteById(chat.getId());
		User currentUser = getCurrentUser();
		long userId = chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id();
		Map<String, Object> response = Map.of("deleted_chat", chat.getId());
		messagingTemplate.convertAndSend("/topic/user/" + userId + "/main", response);
		messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), response);
	}

	private ChatMessage getMessageById(long id) {
		return messagesRepository.findById(id).orElseThrow(() -> new ChatException("Message not found"));
	}

	private void sendMessageOnTopic(ChatMessage message) {
		messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
				messageMapper.toResponseMessageDTO(message));
	}

	private boolean isBlocked(long id, String token) {
		return (boolean) usersService.isBlocked(id, token).get("data");
	}

	private void checkUserInChat(Chat chat) {
		User currentUser = getCurrentUser();
		if (chat.getUser1Id() != currentUser.getId() && chat.getUser2Id() != currentUser.getId()) {
			throw new ChatException("You are not in this chat");
		}
	}

}