package danix.app.chats_service;

import danix.app.chats_service.feign.FilesAPI;
import danix.app.chats_service.models.UsersChatMessage;
import danix.app.chats_service.models.Message;
import danix.app.chats_service.util.SecurityUtil;
import danix.app.chats_service.models.User;
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
    private FilesAPI filesAPI;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private Runnable deleteFunction;

    @Mock
    private MultipartFile testFile;

    @InjectMocks
    private MessagesServiceImpl messagesService;

    private final User currentUser = User.builder().id(1L).build();

    private Message testMessage;

    private final String accessKey = "access_key";

    @BeforeEach
    public void setTestMessage() {
        testMessage = UsersChatMessage.builder()
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
        verify(filesAPI).downloadImage(testMessage.getText(), accessKey);
    }

    @Test
    public void getFileWhenContentTypeVideo() {
        testMessage.setContentType(ContentType.VIDEO);
        messagesService.getFile(testMessage, ContentType.VIDEO);
        verify(filesAPI).downloadVideo(testMessage.getText(), accessKey);
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
        verify(filesAPI).saveImage(testFile, testMessage.getText(), accessKey);
    }

    @Test
    public void saveVideo() {
        messagesService.saveFile(testFile, testMessage, ContentType.VIDEO, deleteFunction);
        verify(filesAPI).saveVideo(testFile, testMessage.getText(), accessKey);
    }

    @Test
    public void saveFileWhenContentTypeText() {
        assertThrows(IllegalArgumentException.class, () ->
                messagesService.saveFile(testFile, testMessage, ContentType.TEXT, deleteFunction));

    }

    @Test
    public void saveImageWhenExceptionInFilesService() {
        doThrow(new RuntimeException()).when(filesAPI).saveImage(testFile, testMessage.getText(), accessKey);
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
        verify(filesAPI).deleteImage(testMessage.getText(), accessKey);
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void deleteMessageWhenContentTypeVideo() {
        mockCurrentUser();
        testMessage.setContentType(ContentType.VIDEO);
        messagesService.deleteMessage(testMessage, "topic", deleteFunction);
        verify(deleteFunction).run();
        verify(filesAPI).deleteVideo(testMessage.getText(), accessKey);
        verify(messagingTemplate).convertAndSend(eq("topic"), any(Map.class));
    }

    @Test
    public void deleteMessageWhenCurrentUserNotSender() {
        mockCurrentUser();
        testMessage.setSenderId(2L);
        assertThrows(ChatException.class, () -> messagesService.deleteMessage(testMessage, "topic", deleteFunction));
    }

    private void mockCurrentUser() {
        when(securityUtil.getCurrentUser()).thenReturn(currentUser);
    }

}
