package danix.app.chats_service.services;

import danix.app.chats_service.dto.ResponseChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.feignClients.FilesService;
import danix.app.chats_service.feignClients.UsersService;
import danix.app.chats_service.models.Chat;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.repositories.ChatsRepository;
import danix.app.chats_service.repositories.MessagesRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.security.UserDetailsImpl;
import danix.app.chats_service.util.ChatException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatsRepository chatsRepository;
    private final MessagesRepository messagesRepository;
    private final FilesService filesService;
    private final UsersService usersService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper mapper;
    private final KafkaTemplate<String, List<String>> kafkaTemplate;

    public List<ResponseChatDTO> getUserChats() {
        User currentUser = getCurrentUser();
        return chatsRepository.findAllByUser1IdOrUser2Id(currentUser.getId(), currentUser.getId()).stream()
                .map(chat -> new ResponseChatDTO(chat.getId(),
                        chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id()))
                .toList();
    }

    public List<ResponseMessageDTO> getChatMessages(long chatId, int page, int count) {
        Chat chat = chatsRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));
        checkUserInChat(chat);
        return messagesRepository.findAllByChat(chat, PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(this::convertToResponseMessageDTO)
                .toList();
    }

    @Transactional
    public void create(long userId, String token) {
        User currentUser = getCurrentUser();
        if (usersService.isBlocked(userId, token)) {
            throw new ChatException("User has blocked you");
        }
        chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), userId).ifPresentOrElse(chat -> {
            throw new ChatException("Chat already exists");
        }, () -> {
            Chat chat = chatsRepository.save(new Chat(currentUser.getId(), userId));
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/main",
                    Map.of("created_chat", chat.getId()));
        });
    }

    @Transactional
    public void sendTextMessage(long chatId, String text, String token) {
        sendMessage(chatId, text, Message.ContentType.TEXT, token);
    }

    @Transactional
    public void sendImage(long chatId, MultipartFile image, String token) {
        String uuid = UUID.randomUUID() + ".jpg";
        Message message = sendMessage(chatId, uuid, Message.ContentType.IMAGE, token);
        try {
            filesService.saveImage(image, uuid);
        } catch (Exception e) {
            messagesRepository.delete(message);
            throw e;
        }
    }

    public Map<String, Object> getImage(long id) {
        Message message = messagesRepository.findById(id)
                .orElseThrow(() -> new ChatException("Message not found"));
        checkUserInChat(message.getChat());
        if (message.getContentType() != Message.ContentType.IMAGE) {
            throw new ChatException("Message is not image");
        }
        return filesService.downloadImage(message.getText());
    }

    private Message sendMessage(long chatId, String text, Message.ContentType contentType, String token) {
        User currentUser = getCurrentUser();
        Chat chat = chatsRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));
        checkUserInChat(chat);
        long userId = chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id();
        if (usersService.isBlocked(userId, token)) {
            throw new ChatException("User has blocked you");
        }
        Message message = messagesRepository.save(Message.builder()
                .chat(chat)
                .text(text)
                .senderId(currentUser.getId())
                .sentTime(LocalDateTime.now())
                .contentType(contentType)
                .build()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), convertToResponseMessageDTO(message));
        return message;
    }

    @Transactional
    public void deleteMessage(long id) {
        User currentUser = getCurrentUser();
        Message message = messagesRepository.findById(id)
                .orElseThrow(() -> new ChatException("Message not found"));
        if (message.getSenderId() != currentUser.getId()) {
            throw new ChatException("You are not owner of this message");
        }
        messagesRepository.delete(message);
        if (message.getContentType() == Message.ContentType.IMAGE) {
            filesService.deleteImage(message.getText());
        }
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
                Map.of("deleted_message", message.getId()));
    }

    @Transactional
    public void delete(long id) {
        Chat chat = chatsRepository.findById(id)
                .orElseThrow(() -> new ChatException("Chat not found"));
        checkUserInChat(chat);
        List<String> images = messagesRepository.findAllByChatAndContentType(chat, Message.ContentType.IMAGE).stream()
                        .map(Message::getText)
                        .toList();
        kafkaTemplate.send("deleted_chat", images);
        chatsRepository.deleteById(chat.getId());
        User currentUser = getCurrentUser();
        long userId = chat.getUser1Id() == currentUser.getId() ? chat.getUser2Id() : chat.getUser1Id();
        Map<String, Object> response = Map.of("deleted_chat", chat.getId());
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/main", response);
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), response);
    }

    private ResponseMessageDTO convertToResponseMessageDTO(Message message) {
        ResponseMessageDTO messageDTO = mapper.map(message, ResponseMessageDTO.class);
        if (message.getContentType() != Message.ContentType.TEXT) {
            messageDTO.setText(null);
        }
        return messageDTO;
    }

    private void checkUserInChat(Chat chat) {
        User currentUser = getCurrentUser();
        if (chat.getUser1Id() != currentUser.getId() && chat.getUser2Id() != currentUser.getId()) {
            throw new ChatException("You are not in this chat");
        }
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.authentication();
    }
}