package danix.app.chats_service;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.mapper.MessageMapper;
import danix.app.chats_service.models.*;
import danix.app.chats_service.repositories.SupportChatsMessagesRepository;
import danix.app.chats_service.repositories.SupportChatsRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.security.UserDetailsImpl;
import danix.app.chats_service.services.MessagesService;
import danix.app.chats_service.services.SupportChatsService;
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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupportChatsServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessagesService messagesService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SupportChatsMessagesRepository messagesRepository;

    @Mock
    private MultipartFile testFile;

    @Mock
    private SupportChatsRepository supportChatsRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Map<ChatType, ChatFactory> factoryMap;

    @Mock
    private ChatFactory factory;

    @InjectMocks
    private SupportChatsService supportChatsService;

    private final User currentUser = User.builder().id(1L).build();

    private SupportChat supportChat;

    @BeforeEach
    public void setSupportChat() {
        supportChat = SupportChat.builder()
                .id(1L)
                .userId(currentUser.getId())
                .adminId(2L)
                .status(SupportChat.Status.WAIT)
                .build();
    }

    @Test
    public void create() {
        mockCurrentUser();
        when(supportChatsRepository.findByUserIdAndStatusIn(eq(currentUser.getId()), any()))
                .thenReturn(Optional.empty());
        when(factoryMap.get(ChatType.SUPPORT_CHAT)).thenReturn(factory);
        when(factory.getChat(any(), any())).thenReturn(supportChat);
        supportChatsService.create("message");
        verify(supportChatsRepository).save(any());
        verify(messagesRepository).save(any());
    }

    @Test
    public void createWhenChatAlreadyExists() {
        mockCurrentUser();
        when(supportChatsRepository.findByUserIdAndStatusIn(eq(currentUser.getId()), any()))
                .thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.create("message"));
    }

    @Test
    public void close() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        supportChatsService.close(supportChat.getId());
        assertEquals(SupportChat.Status.CLOSED, supportChat.getStatus());
        verify(messagingTemplate).convertAndSend(eq("/topic/support/" + supportChat.getId()), any(Map.class));
    }

    @Test
    public void closeWhenChatNotFound() {
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.close(supportChat.getId()));
    }

    @Test
    public void closeChatWhenUserNotExistsInChat() {
        mockCurrentUser();
        supportChat.setUserId(2L);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.close(supportChat.getId()));
    }

    @Test
    public void setStatusToWait() {
        mockCurrentUser();
        supportChat.setStatus(SupportChat.Status.IN_PROCESSING);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        supportChatsService.setStatusToWait(supportChat.getId());
        assertEquals(SupportChat.Status.WAIT, supportChat.getStatus());
        verify(messagingTemplate).convertAndSend(eq("/topic/support/" + supportChat.getId()), any(Map.class));
    }

    @Test
    public void setStatusToWaitWhenChatNotFound() {
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToWait(supportChat.getId()));
    }

    @Test
    public void setStatusToWaitWhenCurrentUserNotOwner() {
        mockCurrentUser();
        supportChat.setUserId(2L);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToWait(supportChat.getId()));
    }

    @Test
    public void setStatusToWaitWhenStatusIsWait() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToWait(supportChat.getId()));
    }

    @Test
    public void setStatusToWaitWhenStatusIsClosed() {
        mockCurrentUser();
        supportChat.setStatus(SupportChat.Status.CLOSED);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToWait(supportChat.getId()));
    }

    @Test
    public void setStatusToProcessing() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        supportChatsService.setStatusToProcessing(supportChat.getId());
        assertEquals(currentUser.getId(), supportChat.getAdminId());
        assertEquals(SupportChat.Status.IN_PROCESSING, supportChat.getStatus());
        verify(messagingTemplate).convertAndSend(eq("/topic/support/" + supportChat.getId()), any(Map.class));
    }

    @Test
    public void setStatusToProcessingWhenChatNotFound() {
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToProcessing(supportChat.getId()));
    }

    @Test
    public void setStatusToProcessingWhenStatusIsClosed() {
        supportChat.setStatus(SupportChat.Status.CLOSED);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToProcessing(supportChat.getId()));
    }

    @Test
    public void setStatusToProcessingWhenStatusIsProcessing() {
        supportChat.setStatus(SupportChat.Status.IN_PROCESSING);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.setStatusToProcessing(supportChat.getId()));
    }

    @Test
    public void sendMessage() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        when(factoryMap.get(ChatType.SUPPORT_CHAT)).thenReturn(factory);
        Message message = SupportChatMessage.builder()
                .id(1L)
                .chat(supportChat)
                .build();
        when(factory.getMessage(any(), any(), any())).thenReturn(message);
        ResponseMessageDTO messageDTO = new ResponseMessageDTO();
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(messageDTO);
        supportChatsService.sendTextMessage("message", supportChat.getId());
        verify(messagesRepository).save(any());
        verify(messagingTemplate).convertAndSend("/topic/support/" + supportChat.getId(), messageDTO);
    }

    @Test
    public void sendMessageWhenChatNotFound() {
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.sendTextMessage("message", supportChat.getId()));
    }

    @Test
    public void sendMessageWhenChatIsClosed() {
        supportChat.setStatus(SupportChat.Status.CLOSED);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.sendTextMessage("message", supportChat.getId()));
    }

    @Test
    public void sendFile() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        when(factoryMap.get(ChatType.SUPPORT_CHAT)).thenReturn(factory);
        Message message = SupportChatMessage.builder()
                .id(1L)
                .chat(supportChat)
                .build();
        when(factory.getMessage(any(), any(), any())).thenReturn(message);
        ResponseMessageDTO messageDTO = new ResponseMessageDTO();
        when(messageMapper.toResponseMessageDTO(any())).thenReturn(messageDTO);
        supportChatsService.sendFile(supportChat.getId(), testFile, ContentType.IMAGE);
        verify(messagesRepository).save(any());
        verify(messagesService).saveFile(eq(testFile), any(), eq(ContentType.IMAGE), any());
        verify(messagingTemplate).convertAndSend("/topic/support/" + supportChat.getId(), messageDTO);
    }

    @Test
    public void sendMessageWhenUserNotExistsInChat() {
        mockCurrentUser();
        supportChat.setUserId(2L);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.sendTextMessage("message", supportChat.getId()));
    }

    @Test
    public void updateMessage() {
        SupportChatMessage testMessage = SupportChatMessage.builder()
                .id(1L)
                .chat(supportChat)
                .build();
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        supportChatsService.updateMessage(testMessage.getId(), "message");
        verify(messagesService).updateMessage(testMessage, "message", "/topic/support/" + supportChat.getId());
    }

    @Test
    public void updateMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.updateMessage(1L, "message"));
    }

    @Test
    public void deleteMessage() {
        SupportChatMessage testMessage = SupportChatMessage.builder()
                .id(1L)
                .chat(supportChat)
                .build();
        when(messagesRepository.findById(testMessage.getId())).thenReturn(Optional.of(testMessage));
        supportChatsService.deleteMessage(testMessage.getId());
        verify(messagesService).deleteMessage(eq(testMessage), eq("/topic/support/" + supportChat.getId()), any());
    }

    @Test
    public void deleteMessageWhenMessageNotFound() {
        when(messagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.deleteMessage(1L));
    }

    @Test
    public void deleteChat() {
        mockCurrentUser();
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        supportChatsService.delete(supportChat.getId());
        verify(messagesService).deleteFiles(any(), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/support/" + supportChat.getId()), any(Map.class));
    }

    @Test
    public void deleteChatWhenChatNotFound() {
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.empty());
        assertThrows(ChatException.class, () -> supportChatsService.delete(supportChat.getId()));
    }

    @Test
    public void deleteChatWhenCurrentUserNotExistsInChat() {
        mockCurrentUser();
        supportChat.setUserId(2L);
        when(supportChatsRepository.findById(supportChat.getId())).thenReturn(Optional.of(supportChat));
        assertThrows(ChatException.class, () -> supportChatsService.delete(supportChat.getId()));
    }

    private void mockCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

}
