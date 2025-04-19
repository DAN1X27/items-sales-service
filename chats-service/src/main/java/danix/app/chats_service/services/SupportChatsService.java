package danix.app.chats_service.services;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.mapper.ChatMapper;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.SupportChatsMessagesRepository;
import danix.app.chats_service.repositories.SupportChatsRepository;
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

import java.util.*;

import static danix.app.chats_service.security.UserDetailsServiceImpl.getCurrentUser;

@Service
@RequiredArgsConstructor
public class SupportChatsService {

	private final SupportChatsRepository chatsRepository;

	private final SupportChatsMessagesRepository messagesRepository;

	private final SimpMessagingTemplate messagingTemplate;

	private final MessagesService messagesService;

	private final KafkaTemplate<String, List<String>> kafkaTemplate;

	private final MessageMapper messageMapper;

	private final ChatMapper chatMapper;

	private static final ChatFactory factory = ChatFactory.getFactory(FactoryType.SUPPORT_CHAT_FACTORY);

	public List<ResponseSupportChatDTO> findAllByUser() {
		long id = getCurrentUser().getId();
		return chatMapper.toResponseSupportChatDTOList(chatsRepository.findAllByUserIdOrAdminId(id, id));
	}

	public List<ResponseSupportChatDTO> findAll(int page, int count, String sort) {
		Sort.Direction sortDirection;
		try {
			sortDirection = Sort.Direction.valueOf(sort.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new ChatException("Invalid sort type");
		}
		List<SupportChat> chats = chatsRepository.findAllByStatus(SupportChat.Status.WAIT, PageRequest.of(page, count,
				Sort.by(sortDirection, "id")));
		return chatMapper.toResponseSupportChatDTOList(chats);
	}

	public List<ResponseMessageDTO> getChatMessages(long id, int page, int count) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		List<Message> messages = messagesRepository.findAllByChat(chat, PageRequest.of(page, count,
				Sort.by(Sort.Direction.DESC, "id")));
		return messageMapper.toResponseMessageDTOList(messages);
	}

	@Transactional
	public DataDTO<Long> create(String message) {
		User user = getCurrentUser();
		List<SupportChat.Status> statuses = List.of(SupportChat.Status.WAIT, SupportChat.Status.IN_PROCESSING);
		chatsRepository.findByUserIdAndStatusIn(user.getId(), statuses).ifPresent(chat -> {
			throw new ChatException("You already have active chat");
		});
		SupportChat chat = chatsRepository.save((SupportChat) factory.createChat(user.getId(), null));
		sendTextMessage(message, chat.getId());
		return new DataDTO<>(chat.getId());
	}

	@Transactional
	public void close(long id) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		if (chat.getStatus() == SupportChat.Status.CLOSED) {
			throw new ChatException("Chat is already closed");
		}
		chat.setStatus(SupportChat.Status.CLOSED);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Transactional
	public void setStatusToWait(long id) {
		SupportChat chat = getById(id);
		if (chat.getUserId() != getCurrentUser().getId()) {
			throw new ChatException("You are not owner of this chat");
		}
		switch (chat.getStatus()) {
			case WAIT -> throw new ChatException("Chat already in wait status");
			case CLOSED -> throw new ChatException("Chat is closed");
		}
		chat.setStatus(SupportChat.Status.WAIT);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Transactional
	public void take(long id) {
		SupportChat chat = getById(id);
		switch (chat.getStatus()) {
			case CLOSED -> throw new ChatException("Chat is closed");
			case IN_PROCESSING -> throw new ChatException("Chat is already in processing status");
		}
		chat.setAdminId(getCurrentUser().getId());
		chat.setStatus(SupportChat.Status.IN_PROCESSING);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Transactional
	public DataDTO<Long> sendTextMessage(String text, long id) {
		SupportChatMessage message = saveMessage(id, text, ContentType.TEXT);
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	public DataDTO<Long> sendFile(long chatId, MultipartFile file, ContentType contentType) {
		String fileName = MessagesService.getFileName(contentType);
		SupportChatMessage message = saveMessage(chatId, fileName, contentType);
		DataDTO<Long> result = messagesService.saveFile(file, message, contentType,
				() -> messagesRepository.delete(message));
		sendMessageOnTopic(message);
		return result;
	}

	public ResponseEntity<?> getFile(long id, ContentType contentType) {
		SupportChatMessage message = getMessageById(id);
		checkAccess(message.getChat());
		return messagesService.getFile(message, contentType);
	}

	private SupportChatMessage saveMessage(long id, String text, ContentType contentType) {
		SupportChat chat = getById(id);
		if (chat.getStatus() == SupportChat.Status.CLOSED) {
			throw new ChatException("Chat is closed");
		}
		checkAccess(chat);
		return messagesRepository.save((SupportChatMessage) factory.createMessage(text, chat, contentType));
	}

	@Transactional
	public void updateMessage(long id, String text) {
		SupportChatMessage message = getMessageById(id);
		String topic = "/topic/support/" + message.getChat().getId();
		messagesService.updateMessage(message, text, topic);
	}

	@Transactional
	public void deleteMessage(long id) {
		SupportChatMessage message = getMessageById(id);
		String topic = "/topic/support/" + message.getChat().getId();
		messagesService.deleteMessage(message, topic, () -> messagesRepository.delete(message));
	}

	@Transactional
	public void delete(long id) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		List<String> files = messagesRepository
			.findAllByChatAndContentTypeIn(chat, List.of(ContentType.IMAGE, ContentType.VIDEO)).stream()
				.map(SupportChatMessage::getText)
				.toList();
		kafkaTemplate.send("deleted_chat", files);
		chatsRepository.deleteById(chat.getId());
		messagingTemplate.convertAndSend("/topic/support/" + chat.getId(),
				Map.of("deleted_chat", chat.getId()));
	}

	private void sendUpdatedStatusMessage(long chatId, SupportChat.Status status) {
		Map<String, Object> message = Map.of(
				"updated_status", status,
				"updater_id", getCurrentUser().getId()
		);
		messagingTemplate.convertAndSend("/topic/support/" + chatId, message);
	}

	private SupportChat getById(long id) {
		return chatsRepository.findById(id).orElseThrow(() -> new ChatException("Chat not found"));
	}

	private SupportChatMessage getMessageById(long id) {
		return messagesRepository.findById(id).orElseThrow(() -> new ChatException("Message not found"));
	}

	private void sendMessageOnTopic(SupportChatMessage message) {
		messagingTemplate.convertAndSend("/topic/support/" + message.getChat().getId(),
				messageMapper.toResponseMessageDTO(message));
	}

	private void checkAccess(SupportChat chat) {
		User user = getCurrentUser();
		if (chat.getUserId() != user.getId() && (chat.getAdminId() == null || !chat.getAdminId().equals(user.getId()))) {
			throw new ChatException("You are not in this chat");
		}
	}

}