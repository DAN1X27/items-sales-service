package danix.app.chats_service.services;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.feignClients.FilesService;
import danix.app.chats_service.models.SupportChat;
import danix.app.chats_service.models.SupportChatMessage;
import danix.app.chats_service.repositories.SupportChatsMessagesRepository;
import danix.app.chats_service.repositories.SupportChatsRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static danix.app.chats_service.security.UserDetailsServiceImpl.getCurrentUser;

@Service
@RequiredArgsConstructor
public class SupportChatService {
    private final SupportChatsRepository chatsRepository;
    private final SupportChatsMessagesRepository messagesRepository;
    private final FilesService filesService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper mapper;
    private final KafkaTemplate<String, List<String>> kafkaTemplate;

    public List<ResponseSupportChatDTO> findAllByUser() {
        long id = getCurrentUser().getId();
        return chatsRepository.findAllByUserIdOrAdminId(id, id).stream()
                .map(chat -> mapper.map(chat, ResponseSupportChatDTO.class))
                .toList();
    }

    public List<ResponseSupportChatDTO> findAll(int page, int count, String sort) {
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ChatException("Invalid sort type");
        }
        return chatsRepository.findAllByStatus(SupportChat.Status.WAIT,
                        PageRequest.of(page, count, Sort.by(sortDirection, "id"))).stream()
                .map(chat -> mapper.map(chat, ResponseSupportChatDTO.class))
                .toList();
    }

    public List<ResponseMessageDTO> getChatMessages(long id, int page, int count) {
        SupportChat chat = getById(id);
        checkAccess(chat);
        return messagesRepository.findAllByChat(chat, PageRequest.of(page, count,
                Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(this::convertToResponseMessageDTO)
                .toList();
    }

    public SupportChat getById(long id) {
        return chatsRepository.findById(id)
                .orElseThrow(() -> new ChatException("Chat not found"));
    }

    @Transactional
    public void create() {
        User user = getCurrentUser();
        List<SupportChat.Status> statuses = List.of(SupportChat.Status.WAIT, SupportChat.Status.IN_PROCESSING);
        chatsRepository.findByUserIdAndStatusIn(user.getId(), statuses).ifPresent(chat -> {
            throw new ChatException("You already have active chat");
        });
        chatsRepository.save(new SupportChat(user.getId(), SupportChat.Status.WAIT));
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
    public void sendTextMessage(String text, long id) {
        sendMessage(id, text, ContentType.TEXT);
    }

    @Transactional
    public void sendImage(MultipartFile image, long id) {
        String uuid = UUID.randomUUID() + ".jpg";
        SupportChatMessage message = sendMessage(id, uuid, ContentType.IMAGE);
        try {
            filesService.saveImage(image, uuid);
        } catch (Exception e) {
            messagesRepository.delete(message);
        }
    }

    private SupportChatMessage sendMessage(long id, String text, ContentType contentType) {
        SupportChat chat = getById(id);
        switch (chat.getStatus()) {
            case WAIT -> throw new ChatException("Chat in wait status");
            case CLOSED -> throw new ChatException("Chat is closed");
        }
        checkAccess(chat);
        SupportChatMessage message = messagesRepository.save(SupportChatMessage.builder()
                .text(text)
                .contentType(contentType)
                .chat(chat)
                .senderId(getCurrentUser().getId())
                .sentTime(LocalDateTime.now())
                .build());
        messagingTemplate.convertAndSend("/topic/support/" + chat.getId(),
                convertToResponseMessageDTO(message));
        return message;
    }

    public Map<String, Object> getImage(long id) {
        SupportChatMessage message = messagesRepository.findById(id)
                .orElseThrow(() -> new ChatException("Message not found"));
        if (message.getContentType() != ContentType.IMAGE) {
            throw new ChatException("Message is not image");
        }
        return filesService.downloadImage(message.getText());
    }

    @Transactional
    public void deleteMessage(long id) {
        SupportChatMessage message = messagesRepository.findById(id)
                .orElseThrow(() -> new ChatException("Message not found"));
        if (message.getSenderId() != getCurrentUser().getId()) {
            throw new ChatException("You are not owner of this message");
        }
        if (message.getContentType() == ContentType.IMAGE) {
            filesService.deleteImage(message.getText());
        }
        messagesRepository.delete(message);
        messagingTemplate.convertAndSend("/topic/support/" + message.getChat().getId(),
                Map.of("deleted_message", message.getId()));
    }

    @Transactional
    public void delete(long id) {
        SupportChat chat = getById(id);
        checkAccess(chat);
        List<String> images = messagesRepository.findAllByChatAndContentType(chat, ContentType.IMAGE).stream()
                .map(SupportChatMessage::getText)
                .toList();
        kafkaTemplate.send("deleted_chat", images);
        chatsRepository.deleteById(chat.getId());
        messagingTemplate.convertAndSend("/topic/support/" + chat.getId(),
                Map.of("deleted_chat", chat.getId()));
    }

    private void sendUpdatedStatusMessage(long chatId, SupportChat.Status status) {
        Map<String, Object> message = new HashMap<>();
        message.put("updated_status", status);
        message.put("updater_id", getCurrentUser().getId());
        messagingTemplate.convertAndSend("/topic/support/" + chatId, message);
    }

    private ResponseMessageDTO convertToResponseMessageDTO(SupportChatMessage message) {
        ResponseMessageDTO messageDTO = mapper.map(message, ResponseMessageDTO.class);
        if (message.getContentType() != ContentType.TEXT) {
            messageDTO.setText(null);
        }
        return messageDTO;
    }

    private void checkAccess(SupportChat chat) {
        User user = getCurrentUser();
        if (chat.getUserId() != user.getId() && !chat.getAdminId().equals(user.getId())) {
            throw new ChatException("You are not in this chat");
        }
    }
}