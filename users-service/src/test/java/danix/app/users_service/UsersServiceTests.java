package danix.app.users_service;


import danix.app.users_service.dto.EmailMessageDTO;
import danix.app.users_service.dto.ResponseUserDTO;
import danix.app.users_service.dto.UpdateInfoDTO;
import danix.app.users_service.feign.AuthenticationAPI;
import danix.app.users_service.feign.FilesAPI;
import danix.app.users_service.mapper.CommentMapper;
import danix.app.users_service.mapper.ReportMapper;
import danix.app.users_service.mapper.UserMapper;
import danix.app.users_service.models.*;
import danix.app.users_service.repositories.*;
import danix.app.users_service.util.SecurityUtil;
import danix.app.users_service.services.impl.UsersServiceImpl;
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
    private TempUsersRepository tempUsersRepository;

    @Mock
    private FilesAPI filesAPI;

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
    private AuthenticationAPI authenticationAPI;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UsersServiceImpl usersService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(usersService, "emailMessageKafkaTemplate", emailMessageKafkaTemplate);
        ReflectionTestUtils.setField(usersService, "longKafkaTemplate", longKafkaTemplate);
    }

    @Test
    public void showWhenUserIsNotBlockedAndCurrentUserIsNotBlocked() {
        User testUser = getTestUser();
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
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
        User user = getTestUser();
        TempUser tempUser = TempUser.builder().email(user.getEmail()).build();
        when(tempUsersRepository.findById(tempUser.getEmail())).thenReturn(Optional.of(tempUser));
        when(userMapper.fromTempUser(tempUser)).thenReturn(user);
        usersService.registrationConfirm(tempUser.getEmail());
        verify(usersRepository).save(user);
        verify(tempUsersRepository).delete(tempUser);
    }

    @Test
    public void updateInfoWhenAllParamsExists() {
        User currentUser = mockCurrentUser();
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        UpdateInfoDTO updateInfoDTO = new UpdateInfoDTO();
        updateInfoDTO.setUsername("new_username");
        usersService.updateInfo(updateInfoDTO);
        assertEquals(currentUser.getUsername(), updateInfoDTO.getUsername());
        assertNotNull(currentUser.getCountry());
        assertNotNull(currentUser.getCity());
    }

    @Test
    public void updateAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        String oldAvatar = currentUser.getAvatar();
        MultipartFile image = new MockMultipartFile("test_image", new byte[0]);
        usersService.updateAvatar(image);
        verify(filesAPI).uploadAvatar(any(), any(), any());
        verify(filesAPI).deleteAvatar(any(), any());
        assertNotEquals(oldAvatar, currentUser.getAvatar());
    }

    @Test
    public void deleteAvatarWhenAvatarEqualsToDefaultAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        String oldAvatar = currentUser.getAvatar();
        MultipartFile image = new MockMultipartFile("test_image", new byte[0]);
        ReflectionTestUtils.setField(usersService, "defaultAvatar", currentUser.getAvatar());
        usersService.updateAvatar(image);
        verify(filesAPI).uploadAvatar(any(), any(), any());
        verify(filesAPI, never()).deleteAvatar(any(), any());
        assertNotEquals(oldAvatar, currentUser.getAvatar());
    }

    @Test
    public void deleteAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        ReflectionTestUtils.setField(usersService, "defaultAvatar", "default");
        usersService.deleteAvatar();
        verify(filesAPI).deleteAvatar(any(), any());
        assertEquals("default", currentUser.getAvatar());
    }

    @Test
    public void deleteAvatarWhenUserHasDefaultAvatar() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        ReflectionTestUtils.setField(usersService, "defaultAvatar", "default");
        currentUser.setAvatar("default");
        assertThrows(UserException.class, () -> usersService.deleteAvatar());
    }

    @Test
    public void addComment() {
        User testUser = getTestUser();
        mockCurrentUser();
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
        mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        assertThrows(UserException.class, () -> usersService.addComment(currentUser.getId(), "test_comment"));
    }

    @Test
    public void deleteComment() {
        User currentUser = getTestCurrentUser();
        mockCurrentUser();
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
        mockCurrentUser();
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
        User currentUser = mockCurrentUser();
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
        User currentUser = mockCurrentUser();
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
        User currentUser = mockCurrentUser();
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
        mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 5));
    }

    @Test
    public void addGradeWhenStarsMoreThen5() {
        User testUser = getTestUser();
        mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 6));
    }

    @Test
    public void addGradeWhenStarsLowerThen1() {
        User testUser = getTestUser();
        mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        assertThrows(UserException.class, () -> usersService.addGrade(testUser.getId(), 0));
    }

    @Test
    public void report() {
        User testUser = getTestUser();
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reportsRepository.findByUserAndSender(testUser, currentUser)).thenReturn(Optional.empty());
        usersService.createReport(testUser.getId(), "test_cause");
        verify(reportsRepository).save(any());
    }

    @Test
    public void reportWhenUserNotFound() {
        when(usersRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.createReport(2L, "test_cause"));
    }

    @Test
    public void reportWhenReportAlreadySent() {
        User testUser = getTestUser();
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reportsRepository.findByUserAndSender(testUser, currentUser)).thenReturn(Optional.of(new Report()));
        assertThrows(UserException.class, () -> usersService.createReport(testUser.getId(), "test_cause"));
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        usersService.delete();
        verify(usersRepository).delete(currentUser);
        verify(filesAPI).deleteAvatar(eq(currentUser.getAvatar()), any());
        verify(longKafkaTemplate).send("deleted_user", currentUser.getId());
    }

    @Test
    public void deleteWhenUserHasDefaultAvatar() {
        User currentUser = mockCurrentUser();
        ReflectionTestUtils.setField(usersService, "defaultAvatar", currentUser.getAvatar());
        when(usersRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        usersService.delete();
        verify(usersRepository).delete(currentUser);
        verify(filesAPI, never()).deleteAvatar(eq(currentUser.getAvatar()), any());
        verify(longKafkaTemplate).send("deleted_user", currentUser.getId());
    }

    @Test
    public void deleteById() {
        User testUser = getTestUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        usersService.delete(testUser.getId());
        verify(usersRepository).deleteById(testUser.getId());
    }

    @Test
    public void deleteByIdWhenUserNotFound() {
        Long id = 1L;
        when(usersRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.delete(id));
    }

    @Test
    public void blockUser() {
        User testUser = getTestUser();
        User currentUser = mockCurrentUser();
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.of(new BlockedUser()));
        assertThrows(UserException.class, () -> usersService.blockUser(testUser.getId()));
    }

    @Test
    public void unblockUser() {
        User testUser = getTestUser();
        User currentUser = mockCurrentUser();
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
        User currentUser = mockCurrentUser();
        when(usersRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(blockedUsersRepository.findByOwnerAndUser(currentUser, testUser)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> usersService.unblockUser(testUser.getId()));
    }

    private User mockCurrentUser() {
        User user = getTestCurrentUser();
        when(securityUtil.getCurrentUser()).thenReturn(user);
        return user;
    }

    private User getTestCurrentUser() {
        return User.builder()
                .id(1L)
                .username("user1")
                .email("user1@gmail.com")
                .city("test_city")
                .country("test_country")
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
                .registeredAt(LocalDateTime.now())
                .avatar("test_avatar")
                .build();
    }
}
