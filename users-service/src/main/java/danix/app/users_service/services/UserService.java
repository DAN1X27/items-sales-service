package danix.app.users_service.services;

import danix.app.users_service.dto.*;
import danix.app.users_service.feignClients.AuthenticationService;
import danix.app.users_service.feignClients.FilesService;
import danix.app.users_service.models.*;
import danix.app.users_service.repositories.*;
import danix.app.users_service.security.UserDetailsImpl;
import danix.app.users_service.util.KafkaMessage;
import danix.app.users_service.util.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
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
public class UserService {

    private final UsersRepository usersRepository;
    private final FilesService filesService;
    private final CommentsRepository commentsRepository;
    private final GradesRepository gradesRepository;
    private final ReportsRepository reportsRepository;
    private final BannedUsersRepository bannedUsersRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final AuthenticationService authenticationService;
    private final KafkaTemplate<String, KafkaMessage> messageKafkaTemplate;
    private final KafkaTemplate<String, Long> longKafkaTemplate;
    @Value("${avatar.default}")
    private String defaultAvatar;

    public User getByEmail(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public User getById(Long id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
    }

    public ResponseUserDTO show(Long id) {
        return convertToResponseDTO(getById(id));
    }

    public UserInfoDTO userInfo() {
        User user = getById(getCurrentUser().getId());
        ResponseUserDTO responseUserDTO = convertToResponseDTO(user);
        return UserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .grade(responseUserDTO.getGrade())
                .country(responseUserDTO.getCountry())
                .city(responseUserDTO.getCity())
                .build();
    }

    private ResponseUserDTO convertToResponseDTO(User user) {
        Double grade = !user.getGrades().isEmpty() ? user.getGrades().stream().mapToDouble(Grade::getStars).sum() / user.getGrades().size() : null;
        return ResponseUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .grade(grade)
                .city(user.getCity())
                .country(user.getCountry())
                .build();
    }

    public List<ResponseCommentDTO> getUserComments(long id, int page, int count) {
        User user = getById(id);
        List<Comment> comments = commentsRepository.findAllByUser(user,
                PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id")));
        return comments.stream()
                .map(comment ->
                        ResponseCommentDTO.builder()
                                .text(comment.getText())
                                .id(comment.getId())
                                .senderId(comment.getOwner().getId())
                                .createdAt(comment.getCreatedAt())
                                .build()
                ).toList();
    }

    @Transactional
    public void temporalRegistration(RegistrationDTO registrationDTO) {
        usersRepository.save(
            User.builder()
                .username(registrationDTO.getUsername())
                .email(registrationDTO.getEmail())
                .password(registrationDTO.getPassword())
                .role(User.Role.ROLE_USER)
                .status(User.Status.TEMPORALLY_REGISTERED)
                .registeredAt(LocalDateTime.now())
                .avatar(defaultAvatar)
                .country(registrationDTO.getCountry())
                .city(registrationDTO.getCity())
                .build()
        );
    }

    @Transactional
    public Long registrationConfirm(String email) {
        User user = getByEmail(email);
        user.setStatus(User.Status.REGISTERED);
        return user.getId();
    }

    @Transactional
    public void updateInfo(UpdateInfoDTO updateInfoDTO) {
        User user = getById(getCurrentUser().getId());
        if (updateInfoDTO.getUsername() != null) {
            if (updateInfoDTO.getUsername().isBlank()) {
                throw new UserException("Username must not be empty");
            }
            user.setUsername(updateInfoDTO.getUsername());
        }
        if (updateInfoDTO.getCountry() != null) {
            if (updateInfoDTO.getCountry().isBlank()) {
                throw new UserException("Country must not be empty");
            }
            user.setCountry(updateInfoDTO.getCountry());
        }
        if (updateInfoDTO.getCity() != null) {
            if (updateInfoDTO.getCity().isBlank()) {
                throw new UserException("City must not be empty");
            }
            user.setCity(updateInfoDTO.getCity());
        }
    }

    @Transactional
    public void updatePassword(String email, String password) {
        User user = getByEmail(email);
        user.setPassword(password);
    }

    @Transactional
    public void deleteTempUser(User user) {
        if (user.getStatus() == User.Status.TEMPORALLY_REGISTERED) {
            usersRepository.delete(user);
        }
    }

    @Transactional
    public void updateAvatar(MultipartFile image) {
        User user = getById(getCurrentUser().getId());
        String oldAvatar = user.getAvatar();
        String fileName = UUID.randomUUID() + ".jpg";
        user.setAvatar(fileName);
        filesService.updateAvatar(image, fileName);
        if (!oldAvatar.equals(defaultAvatar)) {
            filesService.deleteAvatar(oldAvatar);
        }
    }

    public Map<String, Object> downloadAvatar(Long id) {
        User user = getById(id);
        return filesService.downloadAvatar(user.getAvatar());
    }

    @Transactional
    public void deleteAvatar() {
        User user = getById(getCurrentUser().getId());
        if (user.getAvatar().equals(defaultAvatar)) {
            throw new UserException("You already have default avatar");
        }
        filesService.deleteAvatar(user.getAvatar());
        user.setAvatar(defaultAvatar);
    }

    @Transactional
    public void addComment(Long id, String text) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw new UserException("You cant add comment to yourself");
        }
        commentsRepository.save(
            Comment.builder()
                .text(text)
                .owner(currentUser)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build()
        );
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentsRepository.findById(id)
                .orElseThrow(() -> new UserException("Comment not found"));
        if (!comment.getOwner().getId().equals(getCurrentUser().getId())) {
            throw new UserException("You are not owner of this comment");
        }
        commentsRepository.delete(comment);
    }

    @Transactional
    public void addGrade(Long id, int stars) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(user.getId())) {
            throw new UserException("You cant add grade to yourself");
        }
        if (stars > 5 || stars < 1) {
            throw new UserException("Grade must be between 1 and 5");
        }
        gradesRepository.findByUserAndOwner(user, currentUser).ifPresentOrElse(grade ->
            grade.setStars(stars),
            () -> gradesRepository.save(
                 Grade.builder()
                     .stars(stars)
                     .user(user)
                     .owner(currentUser)
                     .build()
            ));
    }

    public List<ResponseReportDTO> reports(int page, int count) {
        return reportsRepository.findAll(PageRequest.of(page, count)).stream()
                .map(report ->
                        ResponseReportDTO.builder()
                            .cause(report.getCause())
                            .userId(report.getUser().getId())
                            .id(report.getId())
                            .build()
                ).toList();
    }

    @Transactional
    public void report(Long id, String cause) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        reportsRepository.findByUserAndSender(user, currentUser).ifPresentOrElse(report -> {
            throw new UserException("You already send report to this user");
        }, () -> reportsRepository.save(
                Report.builder()
                     .cause(cause)
                     .user(user)
                     .sender(currentUser)
                     .createdAt(LocalDateTime.now())
                     .build()
        ));
    }

    @Transactional
    public void deleteReport(Integer id) {
        reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
            throw new UserException("Report not found");
        });
    }

    @Transactional
    public void banUser(Long id, String cause, String token) {
        User user = getById(id);
        bannedUsersRepository.findByUser(user).ifPresent(bannedUser -> {
            throw new UserException("User already banned");
        });
        bannedUsersRepository.save(new BannedUser(cause, user));
        authenticationService.deleteUserTokens(token, id);
        String message = "Your account has been blocked by reason - " + cause;
        messageKafkaTemplate.send("message", new KafkaMessage(user.getEmail(), message));
    }

    @Transactional
    public void unbanUser(Long id) {
        User user = getById(id);
        bannedUsersRepository.findByUser(user).ifPresentOrElse(bannedUsersRepository::delete, () -> {
            throw new UserException("User is not banned");
        });
        String message = "Your account has been unblocked";
        messageKafkaTemplate.send("message", new KafkaMessage(user.getEmail(), message));
    }

    @Transactional
    public void delete() {
        User user = getById(getCurrentUser().getId());
        usersRepository.delete(user);
        if (!user.getAvatar().equals(defaultAvatar)) {
            filesService.deleteAvatar(user.getAvatar());
        }
        longKafkaTemplate.send("deleted_user", user.getId());
    }

    @Transactional
    public void blockUser(Long id) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        blockedUsersRepository.findByOwnerAndUser(currentUser, user).ifPresent(blockedUser -> {
            throw new UserException("User already blocked");
        });
        blockedUsersRepository.save(new BlockedUser(currentUser, user));
    }

    @Transactional
    public void unblockUser(Long id) {
        User user = getById(id);
        User currentUser = getCurrentUser();
        blockedUsersRepository.findByOwnerAndUser(currentUser, user).ifPresentOrElse(blockedUsersRepository::delete, () -> {
            throw new UserException("User is not blocked");
        });
    }

    public List<ResponseBlockedUserDTO> getBlockedUsers() {
        return getById(getCurrentUser().getId()).getBlockedUsers().stream()
                .map(blockedUser -> new ResponseBlockedUserDTO(blockedUser.getUser().getId()))
                .toList();
    }

    public boolean isUserBlocked(Long id) {
        User user = getById(id);
        return blockedUsersRepository.findByOwnerAndUser(user, getCurrentUser()).isPresent();
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getUser();
    }
}