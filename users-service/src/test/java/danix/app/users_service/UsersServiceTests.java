package danix.app.users_service;


import danix.app.users_service.dto.AuthenticationDTO;
import danix.app.users_service.dto.EmailMessageDTO;
import danix.app.users_service.dto.ResponseUserDTO;
import danix.app.users_service.dto.UpdateInfoDTO;
import danix.app.users_service.feign.FilesService;
import danix.app.users_service.mapper.CommentMapper;
import danix.app.users_service.mapper.ReportMapper;
import danix.app.users_service.mapper.UserMapper;
import danix.app.users_service.models.BannedUser;
import danix.app.users_service.models.BlockedUser;
import danix.app.users_service.models.Comment;
import danix.app.users_service.models.User;
import danix.app.users_service.repositories.*;
import danix.app.users_service.security.UserDetailsImpl;
import danix.app.users_service.services.UsersService;
import danix.app.users_service.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UsersServiceTests {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private FilesService filesService;

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private GradesRepository gradesRepository;

    @Mock
    private ReportsRepository reportsRepository;

    @Mock
    private BannedUsersRepository bannedUsersRepository;

    @Mock
    private BlockedUsersRepository blockedUsersRepository;

    @Mock
    private KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

    @Mock
    private KafkaTemplate<String, Long> longKafkaTemplate;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UsersService usersService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(usersService, "emailMessageKafkaTemplate", emailMessageKafkaTemplate);
        ReflectionTestUtils.setField(usersService, "longKafkaTemplate", longKafkaTemplate);
    }

    @Test
    public void getAuthentication() {
        User testUser = getTestUser();
        when(usersRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(userMapper.toAuthenticationDTO(testUser)).thenReturn(new AuthenticationDTO());
        AuthenticationDTO authenticationDTO = usersService.getAuthentication(testUser.getEmail());
        assertNotNull(authenticationDTO);
    }

    @Test
    public void getAuthenticationWhenUserNotFound() {
        String email = "test@gmail.com";
        when(usersRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.getAuthentication(email));
    }

    @Test
    public void getAuthenticationWhenUserIsBanned() {
        User testUser = getTestUser();
        when(usersRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser))
                .thenReturn(Optional.of(BannedUser.builder()
                        .user(testUser)
                        .cause("test_cause")
                        .build()));
        assertThrows(UserException.class, () -> usersService.getAuthentication(testUser.getEmail()));
    }

    @Test
    public void showWhenUserIsNotBlockedAndCurrentUserIsNotBlocked() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockCurrentUser(currentUser);
        when(userMapper.toResponseUserDTO(testUser)).thenReturn(new ResponseUserDTO());
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndUser(testUser, currentUser)).thenReturn(Optional.empty());
        ResponseUserDTO responseUserDTO = usersService.show(testUser.getId());
        assertFalse(responseUserDTO.getIsBlocked());
        assertFalse(responseUserDTO.getIsCurrentUserBlocked());
    }

    @Test
    public void showWhenCurrentUserIsBlocked() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockCurrentUser(currentUser);
        when(userMapper.toResponseUserDTO(testUser)).thenReturn(new ResponseUserDTO());
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.empty());
        when(blockedUsersRepository.findByOwnerAndUser(testUser, currentUser)).thenReturn(Optional.of(new BlockedUser()));
        ResponseUserDTO responseUserDTO = usersService.show(testUser.getId());
        assertFalse(responseUserDTO.getIsBlocked());
        assertTrue(responseUserDTO.getIsCurrentUserBlocked());
    }

    @Test
    public void showWhenUserIsBlocked() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockCurrentUser(currentUser);
        when(userMapper.toResponseUserDTO(testUser)).thenReturn(new ResponseUserDTO());
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        when(blockedUsersRepository.findByOwnerAndUser(testUser, currentUser)).thenReturn(Optional.empty());
        ResponseUserDTO responseUserDTO = usersService.show(testUser.getId());
        assertTrue(responseUserDTO.getIsBlocked());
        assertFalse(responseUserDTO.getIsCurrentUserBlocked());
    }

    @Test
    public void showWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.show(2L));
    }

    @Test
    public void registrationConfirm() {
        User testUser = getTestUser();
        testUser.setStatus(User.Status.TEMPORALLY_REGISTERED);
        when(usersRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        usersService.registrationConfirm(testUser.getEmail());
        assertEquals(User.Status.REGISTERED, testUser.getStatus());
    }

    @Test
    public void updateInfoWhenAllParamsExists() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        UpdateInfoDTO updateInfoDTO = new UpdateInfoDTO();
        updateInfoDTO.setUsername("new_username");
        updateInfoDTO.setCity("new_city");
        updateInfoDTO.setCountry("new_country");
        usersService.updateInfo(updateInfoDTO);
        assertEquals(updateInfoDTO.getUsername(), currentUser.getUsername());
        assertEquals(updateInfoDTO.getCity(), currentUser.getCity());
        assertEquals(updateInfoDTO.getCountry(), currentUser.getCountry());
    }

    @Test
    public void updateInfoWhenOnlyUsernameExists() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        UpdateInfoDTO updateInfoDTO = new UpdateInfoDTO();
        updateInfoDTO.setUsername("new_username");
        usersService.updateInfo(updateInfoDTO);
        assertEquals(currentUser.getUsername(), updateInfoDTO.getUsername());
        assertNotNull(currentUser.getCountry());
        assertNotNull(currentUser.getCity());
    }

    @Test
    public void deleteTempUser() {
        User user = getTestUser();
        user.setStatus(User.Status.TEMPORALLY_REGISTERED);
        usersService.deleteTempUser(user);
        verify(usersRepository).delete(user);
    }

    @Test
    public void deleteTempUserWhenUserIsNotTemporallyRegistered() {
        User user = getTestUser();
        usersService.deleteTempUser(user);
        verify(usersRepository, never()).delete(user);
    }

    @Test
    public void updateAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        String oldAvatar = currentUser.getAvatar();
        MultipartFile image = new MockMultipartFile("test_image", new byte[0]);
        usersService.updateAvatar(image);
        verify(filesService).uploadAvatar(any(), any(), any());
        verify(filesService).deleteAvatar(any(), any());
        assertNotEquals(oldAvatar, currentUser.getAvatar());
    }

    @Test
    public void deleteAvatarWhenAvatarEqualsToDefaultAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        String oldAvatar = currentUser.getAvatar();
        MultipartFile image = new MockMultipartFile("test_image", new byte[0]);
        ReflectionTestUtils.setField(usersService, "defaultAvatar", currentUser.getAvatar());
        usersService.updateAvatar(image);
        verify(filesService).uploadAvatar(any(), any(), any());
        verify(filesService, never()).deleteAvatar(any(), any());
        assertNotEquals(oldAvatar, currentUser.getAvatar());
    }

    @Test
    public void deleteAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        ReflectionTestUtils.setField(usersService, "defaultAvatar", "default");
        usersService.deleteAvatar();
        verify(filesService).deleteAvatar(any(), any());
        assertEquals("default", currentUser.getAvatar());
    }

    @Test
    public void deleteAvatarWhenUserHasDefaultAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        ReflectionTestUtils.setField(usersService, "defaultAvatar", "default");
        currentUser.setAvatar("default");
        assertThrows(UserException.class, () -> usersService.deleteAvatar());
    }

    @Test
    public void addComment() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        usersService.addComment(testUser.getId(), "test_comment");
        verify(commentsRepository).save(any(Comment.class));
    }

    @Test
    public void addCommentWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.addComment(2L, "test_comment"));
    }

    @Test
    public void addCommentToYourself() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        assertThrows(UserException.class, () -> usersService.addComment(currentUser.getId(), "test_comment"));
    }

    @Test
    public void deleteComment() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        Comment comment = Comment.builder()
                .id(1L)
                .owner(currentUser)
                .build();
        when(commentsRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        usersService.deleteComment(comment.getId());
        verify(commentsRepository).delete(comment);
    }

    @Test
    public void deleteWhenCommentNotFound() {
        when(commentsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.deleteComment(1L));
    }

    @Test
    public void deleteCommentWhenCurrentUserIsNotCommentOwner() {
        mockCurrentUser(getTestCurrentUser());
        Comment comment = Comment.builder()
                .id(1L)
                .owner(getTestUser())
                .build();
        when(commentsRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        assertThrows(UserException.class, () -> usersService.deleteComment(comment.getId()));
    }

    private void mockCurrentUser(User user) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(user));
    }

    private static User getTestCurrentUser() {
        return User.builder()
                .id(1L)
                .username("user1")
                .email("user1@gmail.com")
                .city("test_city")
                .country("test_country")
                .role(User.Role.ROLE_USER)
                .status(User.Status.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .avatar("test_avatar")
                .build();
    }

    private static User getTestUser() {
        return User.builder()
                .id(2L)
                .username("user2")
                .email("user2@gmail.com")
                .city("test_city")
                .country("test_country")
                .role(User.Role.ROLE_USER)
                .status(User.Status.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .avatar("test_avatar")
                .build();
    }
}
