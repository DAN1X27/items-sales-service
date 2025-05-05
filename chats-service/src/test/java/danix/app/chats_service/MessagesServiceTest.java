package danix.app.chats_service;

import danix.app.chats_service.feign.FilesService;
import danix.app.chats_service.models.ChatMessage;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.security.User;
import danix.app.chats_service.security.UserDetailsImpl;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessagesServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FilesService filesService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Runnable deleteFunction;

    @Mock
    private MultipartFile testFile;

    @InjectMocks
    private MessagesService messagesService;

    private final User currentUser = User.builder().id(1L).build();

    private Message testMessage;

    private final String accessKey = "access_key";

    @BeforeEach
    public void setTestMessage() {
        testMessage = ChatMessage.builder()
                .id(1L)
                .text("test_message")
                .contentType(ContentType.TEXT)
                .senderId(currentUser.getId())
                .sentTime(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(messagesService, "accessKey", accessKey);
    }

    @Test
    public void getFileWhenContentTypeImage() {
        testMessage.setContentType(ContentType.IMAGE);
        messagesService.getFile(testMessage, ContentType.IMAGE);
        verify(filesService).downloadImage(testMessage.getText(), accessKey);
    }

    @Test
    public void getFileWhenContentTypeVideo() {
        testMessage.setContentType(ContentType.VIDEO);
        messagesService.getFile(testMessage, ContentType.VIDEO);
        verify(filesService).downloadVideo(testMessage.getText(), accessKey);
    }

    @Test
    public void getFileWhenContentTypeImageButMessageIsNotImage() {
        assertThrows(ChatException.class, () -> messagesService.getFile(testMessage, ContentType.IMAGE));
    }

    @Test
    public void getFileWhenContentTypeVideoButMessageIsNotVideo() {
        assertThrows(ChatException.class, () -> messagesService.getFile(testMessage, ContentType.VIDEO));
    }

    @Test
    public void getFileWhenContentTypeText() {
        assertThrows(IllegalArgumentException.class, () -> messagesService.getFile(testMessage, ContentType.TEXT));
    }

    @Test
    public void saveImage() {
        messagesService.saveFile(testFile, testMessage, ContentType.IMAGE, deleteFunction);
        verify(filesService).saveImage(testFile, testMessage.getText(), accessKey);
    }

    @Test
    public void saveVideo() {
        messagesService.saveFile(testFile, testMessage, ContentType.VIDEO, deleteFunction);
        verify(filesService).saveVideo(testFile, testMessage.getText(), accessKey);
    }

    @Test
    public void saveFileWhenContentTypeText() {
        assertThrows(IllegalArgumentException.class, () ->
                messagesService.saveFile(testFile, testMessage, ContentType.TEXT, deleteFunction));

    }

    @Test
    public void saveImageWhenExceptionInFilesService() {
        doThrow(new RuntimeException()).when(filesService).saveImage(testFile, testMessage.getText(), accessKey);
        try {
            messagesService.saveFile(testFile, testMessage, ContentType.IMAGE, deleteFunction);
        } catch (Exception e) {
            verify(deleteFunction).run();
        }
    }

    @Test
    public void updateMessage() {
        mockCurrentUser();
        messagesService.updateMessage(testMessage, "new_message", "topic");
        assertEquals("new_message", testMessage.getText());
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void updateMessageWhenCurrentUserNotSender() {
        mockCurrentUser();
        testMessage.setSenderId(2L);
        assertThrows(ChatException.class, () -> messagesService.updateMessage(testMessage, "new_message", "topic"));
    }

    @Test
    public void updateMessageWhenContentTypeImage() {
        mockCurrentUser();
        testMessage.setContentType(ContentType.IMAGE);
        assertThrows(ChatException.class, () -> messagesService.updateMessage(testMessage, "new_message", "topic"));
    }

    @Test
    public void updateMessageWhenContentTypeVideo() {
        mockCurrentUser();
        testMessage.setContentType(ContentType.VIDEO);
        assertThrows(ChatException.class, () -> messagesService.updateMessage(testMessage, "new_message", "topic"));
    }

    @Test
    public void deleteMessage() {
        mockCurrentUser();
        messagesService.deleteMessage(testMessage, "topic", deleteFunction);
        verify(deleteFunction).run();
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void deleteMessageWhenContentTypeImage() {
        mockCurrentUser();
        testMessage.setContentType(ContentType.IMAGE);
        messagesService.deleteMessage(testMessage, "topic", deleteFunction);
        verify(deleteFunction).run();
        verify(filesService).deleteImage(testMessage.getText(), accessKey);
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void deleteMessageWhenContentTypeVideo() {
        mockCurrentUser();
        testMessage.setContentType(ContentType.VIDEO);
        messagesService.deleteMessage(testMessage, "topic", deleteFunction);
        verify(deleteFunction).run();
        verify(filesService).deleteVideo(testMessage.getText(), accessKey);
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void deleteMessageWhenCurrentUserNotSender() {
        mockCurrentUser();
        testMessage.setSenderId(2L);
        assertThrows(ChatException.class, () -> messagesService.deleteMessage(testMessage, "topic", deleteFunction));
    }

    private void mockCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(currentUser));
    }

}
