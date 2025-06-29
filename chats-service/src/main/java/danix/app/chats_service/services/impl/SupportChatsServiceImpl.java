package danix.app.chats_service.services.impl;

import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.feign.UsersAPI;
import danix.app.chats_service.mapper.ChatMapper;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.SupportChatsMessagesRepository;
import danix.app.chats_service.repositories.SupportChatsRepository;
import danix.app.chats_service.util.SecurityUtil;
import danix.app.chats_service.models.User;
import danix.app.chats_service.services.MessagesService;
import danix.app.chats_service.services.SupportChatsService;
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

import java.util.*;

import static danix.app.chats_service.models.SupportChat.Status.CLOSED;
import static danix.app.chats_service.models.SupportChat.Status.IN_PROCESSING;
import static danix.app.chats_service.models.SupportChat.Status.WAIT;
import static danix.app.chats_service.util.ContentType.TEXT;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatsServiceImpl implements SupportChatsService {

	private final SupportChatsRepository chatsRepository;

	private final SupportChatsMessagesRepository messagesRepository;

	private final SimpMessagingTemplate messagingTemplate;

	private final MessagesService messagesService;

	private final UsersAPI usersAPI;

	private final MessageMapper messageMapper;

	private final ChatMapper chatMapper;

	private final SecurityUtil securityUtil;

	@Override
	public List<ResponseSupportChatDTO> getUserChats(int page, int count) {
		long id = securityUtil.getCurrentUser().getId();
		List<SupportChat> chats = chatsRepository.findAllByUserIdOrAdminId(id, id, PageRequest.of(
				page, count, Sort.by(Sort.Direction.DESC, "id")));
		return chatMapper.toResponseSupportChatDTOList(chats);
	}

	@Override
	public List<ResponseSupportChatDTO> findAll(int page, int count, Sort.Direction sortDirection) {
		List<SupportChat> chats = chatsRepository.findAllByStatus(WAIT, PageRequest.of(page, count,
				Sort.by(sortDirection, "id")));
		return chatMapper.toResponseSupportChatDTOList(chats);
	}

	@Override
	public List<ResponseMessageDTO> getChatMessages(long id, int page, int count) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		List<Message> messages = messagesRepository.findAllByChat(chat, PageRequest.of(page, count,
				Sort.by(Sort.Direction.DESC, "id")));
		return messageMapper.toResponseMessageDTOList(messages);
	}

	@Override
	@Transactional
	public DataDTO<Long> create(String message) {
		User user = securityUtil.getCurrentUser();
		List<SupportChat.Status> statuses = List.of(WAIT, IN_PROCESSING);
		chatsRepository.findByUserIdAndStatusIn(user.getId(), statuses).ifPresent(chat -> {
			throw new ChatException("You already have active chat");
		});
		SupportChat chat = chatMapper.toSupportChat(user.getId());
		chatsRepository.save(chat);
		messagesRepository.save(messageMapper.toSupportChatMessage(message, user.getId(), chat, TEXT));
		return new DataDTO<>(chat.getId());
	}

	@Override
	@Transactional
	public void close(long id) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		if (chat.getStatus() == CLOSED) {
			throw new ChatException("Chat is already closed");
		}
		chat.setStatus(CLOSED);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Override
	@Transactional
	public void setStatusToWait(long id) {
		SupportChat chat = getById(id);
		if (chat.getUserId() != securityUtil.getCurrentUser().getId()) {
			throw new ChatException("You are not owner of this chat");
		}
		switch (chat.getStatus()) {
			case WAIT -> throw new ChatException("Chat already in wait status");
			case CLOSED -> throw new ChatException("Chat is closed");
		}
		chat.setStatus(WAIT);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Override
	@Transactional
	public void setStatusToProcessing(long id) {
		SupportChat chat = getById(id);
		switch (chat.getStatus()) {
			case CLOSED -> throw new ChatException("Chat is closed");
			case IN_PROCESSING -> throw new ChatException("Chat is already in processing status");
		}
		chat.setAdminId(securityUtil.getCurrentUser().getId());
		chat.setStatus(IN_PROCESSING);
		sendUpdatedStatusMessage(chat.getId(), chat.getStatus());
	}

	@Override
	@Transactional
	public DataDTO<Long> sendTextMessage(long id, String text, String token) {
		SupportChatMessage message = saveMessage(id, text, TEXT, token);
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	@Override
	public DataDTO<Long> sendFile(long chatId, MultipartFile file, String token, ContentType contentType) {
		String fileName = messagesService.getFileName(contentType);
		SupportChatMessage message = saveMessage(chatId, fileName, contentType, token);
		messagesService.saveFile(file, message, contentType, () -> messagesRepository.delete(message));
		sendMessageOnTopic(message);
		return new DataDTO<>(message.getId());
	}

	@Override
	public ResponseEntity<?> getFile(long id, ContentType contentType) {
		SupportChatMessage message = getMessageById(id);
		checkAccess(message.getChat());
		return messagesService.getFile(message, contentType);
	}

	private SupportChatMessage saveMessage(long id, String text, ContentType contentType, String token) {
		User currentUser = securityUtil.getCurrentUser();
		SupportChat chat = getById(id);
		if (chat.getStatus() == CLOSED) {
			throw new ChatException("Chat is closed");
		}
		checkAccess(chat);
		long userId = chat.getUserId() == currentUser.getId() ? chat.getAdminId() : chat.getUserId();
		boolean isBlocked = usersAPI.isBlockedByUser(userId, token).get("data");
		if (isBlocked) {
			throw new ChatException("User blocked you");
		}
		SupportChatMessage message = messageMapper.toSupportChatMessage(text, currentUser.getId(), chat, contentType);
		messagesRepository.save(message);
		return message;
	}

	@Override
	@Transactional
	public void updateMessage(long id, String text) {
		SupportChatMessage message = getMessageById(id);
		String topic = "/topic/support/" + message.getChat().getId();
		messagesService.updateMessage(message, text, topic);
	}

	@Override
	@Transactional
	public void deleteMessage(long id) {
		SupportChatMessage message = getMessageById(id);
		String topic = "/topic/support/" + message.getChat().getId();
		messagesService.deleteMessage(message, topic, () -> messagesRepository.delete(message));
	}

	@Override
	@Transactional
	public void delete(long id) {
		SupportChat chat = getById(id);
		checkAccess(chat);
		messagesService.deleteFiles(
				page -> messagesRepository.findAllByChatAndContentTypeIsNot(chat, TEXT, PageRequest.of(page, 50)),
				() -> chatsRepository.deleteById(id)
		);
		messagingTemplate.convertAndSend("/topic/support/" + chat.getId(),
				Map.of("deleted_chat", chat.getId()));
	}

	private void sendUpdatedStatusMessage(long chatId, SupportChat.Status status) {
		Map<String, Object> message = Map.of(
				"updated_status", status,
				"updater_id", securityUtil.getCurrentUser().getId()
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
		User user = securityUtil.getCurrentUser();
		if (chat.getUserId() != user.getId() && (chat.getAdminId() == null || !chat.getAdminId().equals(user.getId()))) {
			throw new ChatException("You are not in this chat");
		}
	}
}