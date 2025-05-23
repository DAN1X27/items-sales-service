package danix.app.chats_service;

import danix.app.chats_service.dto.ResponseChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.feign.UsersService;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.UsersChatsMessagesRepository;
import danix.app.chats_service.repositories.UsersChatsRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.security.UserDetailsImpl;
import danix.app.chats_service.services.UsersChatsService;
import danix.app.chats_service.services.MessagesService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsersChatsServiceTest {

    @Mock
    private UsersChatsRepository chatsRepository;

    @Mock
    private UsersChatsMessagesRepository messagesRepository;

    @Mock
    private UsersService usersService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessagesService messagesService;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MultipartFile testFile;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Map<ChatType, ChatFactory> factoryMap;

    @Mock
    private ChatFactory factory;

    @InjectMocks
    private UsersChatsService chatsService;

    private final User currentUser = User.builder().id(1L).build();

    private final UsersChat chat = UsersChat.builder()
            .id(1L)
            .user1Id(1L)
            .user2Id(2L)
            .build();

    @BeforeEach
    public void setUp() {
        currentUser.setId(1L);
    }

    @Test
    public void getUserChats() {
        mockCurrentUser();
        List<UsersChat> chats = List.of(
                UsersChat.builder().user1Id(currentUser.getId()).user2Id(2L).build(),
                UsersChat.builder().user1Id(2L).user2Id(currentUser.getId()).build()
        );
        when(chatsRepository.findAllByUser1IdOrUser2Id(currentUser.getId(), currentUser.getId())).thenReturn(chats);
        List<ResponseChatDTO> responseChats = chatsService.getUserChats();
        responseChats.forEach(chat -> assertEquals(2L, chat.getUserId()));
    }

    @Test
    public void create() {
        mockCurrentUser();
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", false));
        when(chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), 2L)).thenReturn(Optional.empty());
        when(factoryMap.get(ChatType.USERS_CHAT)).thenReturn(factory);
        when(chatsRepository.save(any())).thenReturn(chat);
        chatsService.create(2L, "token");
        verify(chatsRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/main"), any(Map.class));
    }

    @Test
    public void createWhenCurrentUserBlocked() {
        mockCurrentUser();
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", true));
        assertThrows(ChatException.class, () -> chatsService.create(2L, "token"));
    }

    @Test
    public void createWhenChatAlreadyExists() {
        mockCurrentUser();
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", false));
        when(chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), 2L)).thenReturn(Optional.of(new UsersChat()));
        assertThrows(ChatException.class, () -> chatsService.create(2L, "token"));
    }

    @Test
    public void sendTextMessage() {
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", false));
        when(factoryMap.get(ChatType.USERS_CHAT)).thenReturn(factory);
        when(messagesRepository.save(any())).thenReturn(ChatMessage.builder().chat(chat).build());
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(new ResponseMessageDTO());
        chatsService.sendTextMessage(chat.getId(), "text", "token");
        verify(messagesRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), any(ResponseMessageDTO.class));
    }

    @Test
    public void sentTextMessageWhenChatNotFound() {
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text", "token"));
    }

    @Test
    public void sendTextMessageWhenCurrentUserBlocked() {
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", true));
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text", "token"));
    }

    @Test
    public void sendTextMessageWhenCurrentUserNotInChat() {
        currentUser.setId(3L);
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text", "token"));
    }

    @Test
    public void sendFile() {
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersService.isBlocked(2L, "token")).thenReturn(Map.of("data", false));
        when(messagesRepository.save(any())).thenReturn(ChatMessage.builder().chat(chat).build());
        when(factoryMap.get(ChatType.USERS_CHAT)).thenReturn(factory);
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(new ResponseMessageDTO());
        chatsService.sendFile(chat.getId(), testFile, "token", ContentType.IMAGE);
        verify(messagesRepository).save(any());
        verify(messagesService).saveFile(eq(testFile), any(), eq(ContentType.IMAGE), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), any(ResponseMessageDTO.class));
    }

    @Test
    public void updateMessage() {
        ChatMessage message = ChatMessage.builder().id(1L).chat(chat).build();
        when(messagesRepository.findById(message.getId())).thenReturn(Optional.of(message));
        chatsService.updateMessage(message.getId(), "text");
        verify(messagesService).updateMessage(message, "text", "/topic/chat/" + chat.getId());
    }

    @Test
    public void updateMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.updateMessage(1L, "text"));
    }

    @Test
    public void deleteMessage() {
        ChatMessage message = ChatMessage.builder().id(1L).chat(chat).build();
        when(messagesRepository.findById(message.getId())).thenReturn(Optional.of(message));
        chatsService.deleteMessage(message.getId());
        verify(messagesService).deleteMessage(eq(message), eq("/topic/chat/" + chat.getId()), any());
    }

    @Test
    public void deleteMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.deleteMessage(1L));
    }

    @Test
    public void deleteChat() {
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        mockCurrentUser();
        chatsService.delete(chat.getId());
        Map<String, Object> response = Map.of("deleted_chat", chat.getId());
        verify(messagesService).deleteFiles(any(), any());
        verify(messagingTemplate).convertAndSend("/topic/user/" + chat.getUser2Id() + "/main", response);
        verify(messagingTemplate).convertAndSend("/topic/chat/" + chat.getId(), response);
    }

    @Test
    public void deleteWhenChatNotFound() {
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.delete(chat.getId()));
    }

    @Test
    public void deleteChatWhenUserNotInChat() {
        currentUser.setId(3L);
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        assertThrows(ChatException.class, () -> chatsService.delete(chat.getId()));
    }

    private void mockCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

}