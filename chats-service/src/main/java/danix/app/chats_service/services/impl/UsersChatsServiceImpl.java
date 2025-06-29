package danix.app.chats_service.services.impl;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseUsersChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.feign.UsersAPI;
import danix.app.chats_service.mapper.ChatMapper;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.UsersChatsRepository;
import danix.app.chats_service.repositories.UsersChatsMessagesRepository;
import danix.app.chats_service.util.SecurityUtil;
import danix.app.chats_service.models.User;
import danix.app.chats_service.services.MessagesService;
import danix.app.chats_service.services.UsersChatsService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersChatsServiceImpl implements UsersChatsService {

	private final UsersChatsRepository chatsRepository;

	private final UsersChatsMessagesRepository messagesRepository;

	private final UsersAPI usersAPI;

	private final SimpMessagingTemplate messagingTemplate;

	private final MessagesService messagesService;

	private final MessageMapper messageMapper;

	private final ChatMapper chatMapper;

	private final SecurityUtil securityUtil;

	@Override
	public List<ResponseUsersChatDTO> getUserChats(int page, int count) {
		long id = securityUtil.getCurrentUser().getId();
		List<UsersChat> chats = chatsRepository.findAllByUser1IdOrUser2Id(id, id,
				PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id")));
		return chatMapper.toResponseUsersChatDTOList(chats);
	}

	@Override
	public List<ResponseMessageDTO> getChatMessages(long chatId, int page, int count) {
		UsersChat chat = chatsRepository.findById(chatId).orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		List<Message> messages = messagesRepository.findAllByChat(chat, PageRequest.of(page, count,
				Sort.by(Sort.Direction.DESC, "id")));
		return messageMapper.toResponseMessageDTOList(messages);
	}

	@Override
	@Transactional
	public DataDTO<Long> create(long userId, String token) {
		User currentUser = securityUtil.getCurrentUser();
		if (isBlocked(userId, token)) {
			throw new ChatException("User has blocked you");
		}
		chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), userId).ifPresent(chat -> {
			throw new ChatException("Chat already exists");
		});
		UsersChat chat = chatMapper.toUsersChat(currentUser.getId(), userId);
		chatsRepository.save(chat);
		messagingTemplate.convertAndSend("/topic/user/" + userId + "/main",
				Map.of("created_chat", chat.getId()));
		return new DataDTO<>(chat.getId());
	}

	@Override
	@Transactional
	public DataDTO<Long> sendTextMessage(long chatId, String text, String token) {
		UsersChatMessage message = saveMessage(chatId, text, ContentType.TEXT, token);
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	@Override
	@Transactional
	public DataDTO<Long> sendFile(long chatId, MultipartFile file, String token, ContentType contentType) {
		String fileName = messagesService.getFileName(contentType);
		UsersChatMessage message = saveMessage(chatId, fileName, contentType, token);
		messagesService.saveFile(file, message, contentType, () -> messagesRepository.delete(message));
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	@Override
	public ResponseEntity<?> getFile(long id, ContentType contentType) {
		UsersChatMessage message = getMessageById(id);
		checkUserInChat(message.getChat());
		return messagesService.getFile(message, contentType);
	}

	private UsersChatMessage saveMessage(long chatId, String text, ContentType contentType, String token) {
		User currentUser = securityUtil.getCurrentUser();
		UsersChat chat = chatsRepository.findById(chatId).orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		long userId = chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id();
		if (isBlocked(userId, token)) {
			throw new ChatException("User blocked you");
		}
		UsersChatMessage message = messageMapper.toUsersChatMessage(text, currentUser.getId(), chat, contentType);
		messagesRepository.save(message);
		return message;
	}

	@Override
	@Transactional
	public void updateMessage(long id, String text) {
		UsersChatMessage message = getMessageById(id);
		messagesService.updateMessage(message, text, "/topic/chat/" + message.getChat().getId());
	}

	@Override
	@Transactional
	public void deleteMessage(long id) {
		UsersChatMessage message = getMessageById(id);
		String topic = "/topic/chat/" + message.getChat().getId();
		messagesService.deleteMessage(message, topic, () -> messagesRepository.delete(message));
	}

	@Override
	@Transactional
	public void delete(long id) {
		UsersChat chat = chatsRepository.findById(id)
				.orElseThrow(() -> new ChatException("Chat not found"));
		checkUserInChat(chat);
		messagesService.deleteFiles(
				page -> messagesRepository.findAllByChatAndContentTypeIsNot(chat, ContentType.TEXT,
						PageRequest.of(page, 50)),
				() -> chatsRepository.deleteById(id)
		);
		User currentUser = securityUtil.getCurrentUser();
		long userId = chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id();
		Map<String, Object> response = Map.of("deleted_chat", chat.getId());
		messagingTemplate.convertAndSend("/topic/user/" + userId + "/main", response);
		messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), response);
	}

	private UsersChatMessage getMessageById(long id) {
		return messagesRepository.findById(id)
				.orElseThrow(() -> new ChatException("Message not found"));
	}

	private void sendMessageOnTopic(UsersChatMessage message) {
		messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
				messageMapper.toResponseMessageDTO(message));
	}

	private boolean isBlocked(long id, String token) {
		Map<String, Boolean> response = usersAPI.isBlockedByUser(id, token);
		return Objects.requireNonNull(response).get("data");
	}

	private void checkUserInChat(UsersChat chat) {
		User currentUser = securityUtil.getCurrentUser();
		if (chat.getUser1Id() != currentUser.getId() && chat.getUser2Id() != currentUser.getId()) {
			throw new ChatException("You are not in this chat");
		}
	}

}