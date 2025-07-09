package modules;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.feign.UsersAPI;
import danix.app.chats_service.mapper.ChatMapper;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.UsersChatsMessagesRepository;
import danix.app.chats_service.repositories.UsersChatsRepository;
import danix.app.chats_service.util.EncryptUtil;
import danix.app.chats_service.util.SecurityUtil;
import danix.app.chats_service.models.User;
import danix.app.chats_service.services.impl.UsersChatsServiceImpl;
import danix.app.chats_service.services.impl.MessagesServiceImpl;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import util.TestUtil;

import java.util.Map;
import java.util.Optional;

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
    private UsersAPI usersAPI;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessagesServiceImpl messagesService;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MultipartFile testFile;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private EncryptUtil encryptUtil;

    @InjectMocks
    private UsersChatsServiceImpl chatsService;

    private final User currentUser = TestUtil.getTestCurrentUser();

    private static final String token = "token";

    private static final UsersChat chat;

    static {
        chat = TestUtil.getTestUsersChat();
        chat.setId(1L);
    }

    @BeforeEach
    public void setUp() {
        currentUser.setId(1L);
    }

    @Test
    public void create() {
        mockCurrentUser();
        mockJwt();
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", false));
        when(chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), 2L)).thenReturn(Optional.empty());
        when(chatMapper.toUsersChat(any(), any())).thenReturn(chat);
        chatsService.create(2L);
        verify(chatsRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/main"), any(Map.class));
    }

    @Test
    public void createWhenCurrentUserBlocked() {
        mockCurrentUser();
        mockJwt();
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", true));
        assertThrows(ChatException.class, () -> chatsService.create(2L));
    }

    @Test
    public void createWhenChatAlreadyExists() {
        mockCurrentUser();
        mockJwt();
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", false));
        when(chatsRepository.findByUser1IdAndUser2Id(currentUser.getId(), 2L)).thenReturn(Optional.of(new UsersChat()));
        assertThrows(ChatException.class, () -> chatsService.create(2L));
    }

    @Test
    public void sendTextMessage() {
        mockCurrentUser();
        mockJwt();
        String message = "message";
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", false));
        when(messageMapper.toUsersChatMessage(any(), any(), any(), any())).thenReturn(UsersChatMessage.builder().chat(chat).build());
        when(encryptUtil.encrypt(message)).thenReturn(message);
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(new ResponseMessageDTO());
        chatsService.sendTextMessage(chat.getId(), message);
        verify(messagesRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), any(ResponseMessageDTO.class));
    }

    @Test
    public void sentTextMessageWhenChatNotFound() {
        mockCurrentUser();
        mockJwt();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text"));
    }

    @Test
    public void sendTextMessageWhenCurrentUserBlocked() {
        mockCurrentUser();
        mockJwt();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", true));
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text"));
    }

    @Test
    public void sendTextMessageWhenCurrentUserNotInChat() {
        currentUser.setId(3L);
        mockCurrentUser();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        assertThrows(ChatException.class, () -> chatsService.sendTextMessage(chat.getId(), "text"));
    }

    @Test
    public void sendFile() {
        mockCurrentUser();
        mockJwt();
        when(chatsRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(usersAPI.isBlockedByUser(2L, token)).thenReturn(Map.of("data", false));
        when(messagesRepository.save(any())).thenReturn(UsersChatMessage.builder().chat(chat).build());
        when(messageMapper.toUsersChatMessage(any(), any(), any(), any())).thenReturn(UsersChatMessage.builder().chat(chat).build());
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(new ResponseMessageDTO());
        chatsService.sendFile(chat.getId(), testFile, ContentType.IMAGE);
        verify(messagesRepository).save(any());
        verify(messagesService).saveFile(eq(testFile), any(), eq(ContentType.IMAGE), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), any(ResponseMessageDTO.class));
    }

    @Test
    public void updateMessage() {
        String text = "message";
        UsersChatMessage message = UsersChatMessage.builder().id(1L).chat(chat).build();
        when(messagesRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(encryptUtil.encrypt(text)).thenReturn(text);
        chatsService.updateMessage(message.getId(), text);
        verify(messagesService).updateMessage(message, text, "/topic/chat/" + chat.getId());
    }

    @Test
    public void updateMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> chatsService.updateMessage(1L, "text"));
    }

    @Test
    public void deleteMessage() {
        UsersChatMessage message = UsersChatMessage.builder().id(1L).chat(chat).build();
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
        when(securityUtil.getCurrentUser()).thenReturn(currentUser);
    }

    private void mockJwt() {
        when(securityUtil.getJwt()).thenReturn(token);
    }

}