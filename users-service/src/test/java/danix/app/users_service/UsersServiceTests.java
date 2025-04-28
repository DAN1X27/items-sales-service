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
import danix.app.users_service.models.Grade;
import danix.app.users_service.models.Report;
import danix.app.users_service.models.User;
import danix.app.users_service.repositories.BannedUsersRepository;
import danix.app.users_service.repositories.BlockedUsersRepository;
import danix.app.users_service.repositories.CommentsRepository;
import danix.app.users_service.repositories.GradesRepository;
import danix.app.users_service.repositories.ReportsRepository;
import danix.app.users_service.repositories.UsersRepository;
import danix.app.users_service.security.UserDetailsImpl;
import danix.app.users_service.services.UsersService;
import danix.app.users_service.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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

    @Test
    public void addGrade() {
        User testUser = getTestUser();
        testUser.setGrades(Collections.singletonList(Grade.builder().stars(5).build()));
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(gradesRepository.findByUserAndOwner(testUser, currentUser)).thenReturn(Optional.empty());
        usersService.addGrade(testUser.getId(), 5);
        verify(gradesRepository).save(any());
        assertEquals(5, testUser.getGrade());
    }

    @Test
    public void addGradeWhenUserHasGrades() {
        User testUser = getTestUser();
        testUser.setGrades(List.of(
                Grade.builder().stars(5).build(),
                Grade.builder().stars(4).build(),
                Grade.builder().stars(3).build(),
                Grade.builder().stars(2).build()
        ));
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(gradesRepository.findByUserAndOwner(testUser, currentUser)).thenReturn(Optional.empty());
        usersService.addGrade(testUser.getId(), 2);
        verify(gradesRepository).save(any());
        assertEquals(3.5, testUser.getGrade());
    }

    @Test
    public void addGradeWhenUserHasGradeByCurrentUser() {
        User testUser = getTestUser();
        testUser.setGrades(Collections.singletonList(Grade.builder().stars(5).build()));
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        Grade grade = Grade.builder().stars(3).build();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(gradesRepository.findByUserAndOwner(testUser, currentUser)).thenReturn(Optional.of(grade));
        usersService.addGrade(testUser.getId(), 5);
        verify(gradesRepository, never()).save(any());
        assertEquals(5, grade.getStars());
    }

    @Test
    public void addGradeWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.addGrade(2L, 5));
    }

    @Test
    public void addGradeYourself() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        testUser.setId(currentUser.getId());
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 5));
    }

    @Test
    public void addGradeWhenStarsMoreThen5() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 6));
    }

    @Test
    public void addGradeWhenStarsLowerThen1() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 0));
    }

    @Test
    public void report() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reportsRepository.findByUserAndSender(testUser, currentUser)).thenReturn(Optional.empty());
        usersService.report(testUser.getId(), "test_cause");
        verify(reportsRepository).save(any());
    }

    @Test
    public void reportWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.report(2L, "test_cause"));
    }

    @Test
    public void reportWhenReportAlreadySent() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reportsRepository.findByUserAndSender(testUser, currentUser)).thenReturn(Optional.of(new Report()));
        assertThrows(UserException.class, () -> usersService.report(testUser.getId(), "test_cause"));
    }

    @Test
    public void deleteReport() {
        Report report = new Report();
        when(reportsRepository.findById(1)).thenReturn(Optional.of(report));
        usersService.deleteReport(1);
        verify(reportsRepository).delete(report);
    }

    @Test
    public void deleteReportWhenReportNotFound() {
        when(reportsRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.deleteReport(1));
    }

    @Test
    public void banUser() {
        User testUser = getTestUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser)).thenReturn(Optional.empty());
        usersService.banUser(testUser.getId(), "test_cause");
        verify(bannedUsersRepository).save(any());
        verify(longKafkaTemplate).send(eq("deleted_user_tokens"), any());
        verify(emailMessageKafkaTemplate).send(eq("message"), any());
    }

    @Test
    public void banUserWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.banUser(2L, "test_cause"));
    }

    @Test
    public void banUserWhenUserAlreadyBanned() {
        User testUser = getTestUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser)).thenReturn(Optional.of(new BannedUser()));
        assertThrows(UserException.class, () -> usersService.banUser(testUser.getId(), "test_cause"));
    }

    @Test
    public void unbanUser() {
        User testUser = getTestUser();
        BannedUser bannedUser = BannedUser.builder().build();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser)).thenReturn(Optional.of(bannedUser));
        usersService.unbanUser(testUser.getId());
        verify(bannedUsersRepository).delete(bannedUser);
        verify(emailMessageKafkaTemplate).send(eq("message"), any());
    }

    @Test
    public void unbanUserWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.unbanUser(2L));
    }

    @Test
    public void unbanUserWhenUserNotBanned() {
        User testUser = getTestUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(bannedUsersRepository.findByUser(testUser)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.unbanUser(testUser.getId()));
    }

    @Test
    public void delete() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        usersService.delete();
        verify(usersRepository).delete(currentUser);
        verify(filesService).deleteAvatar(eq(currentUser.getAvatar()), any());
        verify(longKafkaTemplate).send("deleted_user", currentUser.getId());
    }

    @Test
    public void deleteWhenUserHasDefaultAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        ReflectionTestUtils.setField(usersService, "defaultAvatar", currentUser.getAvatar());
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        usersService.delete();
        verify(usersRepository).delete(currentUser);
        verify(filesService, never()).deleteAvatar(eq(currentUser.getAvatar()), any());
        verify(longKafkaTemplate).send("deleted_user", currentUser.getId());
    }

    @Test
    public void blockUser() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.empty());
        usersService.blockUser(testUser.getId());
        verify(blockedUsersRepository).save(any());
    }

    @Test
    public void blockUserWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.blockUser(2L));
    }

    @Test
    public void blockUserWhenUserAlreadyBlocked() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(UserException.class, () -> usersService.blockUser(testUser.getId()));
    }

    @Test
    public void unblockUser() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        usersService.unblockUser(testUser.getId());
        verify(blockedUsersRepository).delete(any());
    }

    @Test
    public void unblockUserWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.unblockUser(2L));
    }

    @Test
    public void unblockUserWhenUserIsNotBlocked() {
        User testUser = getTestUser();
        User currentUser = getTestCurrentUser();
        mockCurrentUser(currentUser);
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.unblockUser(testUser.getId()));
    }

    private void mockCurrentUser(User user) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(user));
    }

    private User getTestCurrentUser() {
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

    private User getTestUser() {
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
