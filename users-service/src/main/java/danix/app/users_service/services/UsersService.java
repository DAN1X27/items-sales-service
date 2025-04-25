package danix.app.users_service.services;

import danix.app.users_service.dto.*;
import danix.app.users_service.feign.FilesService;
import danix.app.users_service.mapper.CommentMapper;
import danix.app.users_service.mapper.ReportMapper;
import danix.app.users_service.mapper.UserMapper;
import danix.app.users_service.models.*;
import danix.app.users_service.repositories.*;
import danix.app.users_service.security.UserDetailsImpl;
import danix.app.users_service.dto.EmailMessageDTO;
import danix.app.users_service.util.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersRepository usersRepository;

	private final FilesService filesService;

	private final CommentsRepository commentsRepository;

	private final GradesRepository gradesRepository;

	private final ReportsRepository reportsRepository;

	private final BannedUsersRepository bannedUsersRepository;

	private final BlockedUsersRepository blockedUsersRepository;

	private final KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

	private final KafkaTemplate<String, Long> longKafkaTemplate;

	private final UserMapper userMapper;

	private final ReportMapper reportMapper;

	private final CommentMapper commentMapper;

	@Value("${avatar.default}")
	private String defaultAvatar;

	@Value("${access_key}")
	private String accessKey;

	public User getByEmail(String email) {
		return usersRepository.findByEmail(email).orElseThrow(() -> new UserException("User not found"));
	}

	public User getById(Long id) {
		return usersRepository.findById(id).orElseThrow(() -> new UserException("User not found"));
	}

	public AuthenticationDTO getAuthentication(String email) {
		User user = getByEmail(email);
		bannedUsersRepository.findByUser(user).ifPresent(bannedUser -> {
			throw new UserException("Your account has been banned due to: " + bannedUser.getCause());
		});
		return userMapper.toAuthenticationDTO(user);
	}

	public ResponseUserDTO show(Long id) {
		User user = getById(id);
		User currentUser = getCurrentUser();
		ResponseUserDTO responseUserDTO = userMapper.toResponseUserDTO(user);
		responseUserDTO.setIsBlocked(blockedUsersRepository
				.findByOwnerAndUser(currentUser, user).isPresent());
		responseUserDTO.setIsCurrentUserBlocked(blockedUsersRepository
				.findByOwnerAndUser(user, currentUser).isPresent());
		return responseUserDTO;
	}

	public UserInfoDTO userInfo() {
		User user = getById(getCurrentUser().getId());
		return userMapper.toUserInfoDTO(user);
	}

	public List<ResponseCommentDTO> getUserComments(long id, int page, int count) {
		User user = getById(id);
		List<Comment> comments = commentsRepository.findAllByUser(user,
				PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id")));
		return commentMapper.toResponseCommentDTOList(comments);
	}

	@Transactional
	public void temporalRegistration(RegistrationDTO registrationDTO) {
		usersRepository.save(User.builder()
			.username(registrationDTO.getUsername())
			.email(registrationDTO.getEmail())
			.password(registrationDTO.getPassword())
			.role(User.Role.ROLE_USER)
			.status(User.Status.TEMPORALLY_REGISTERED)
			.registeredAt(LocalDateTime.now())
			.avatar(defaultAvatar)
			.country(registrationDTO.getCountry())
			.city(registrationDTO.getCity())
			.build());
	}

	@Transactional
	public DataDTO<Long> registrationConfirm(String email) {
		User user = getByEmail(email);
		user.setStatus(User.Status.REGISTERED);
		return new DataDTO<>(user.getId());
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
	public void updatePassword(User user, String password) {
		user.setPassword(password);
	}

	@Transactional
	public void updateEmail(String email) {
		User user = getById(getCurrentUser().getId());
		user.setEmail(email);
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
		filesService.uploadAvatar(image, fileName, accessKey);
		if (!oldAvatar.equals(defaultAvatar)) {
			filesService.deleteAvatar(oldAvatar, accessKey);
		}
	}

	public ResponseEntity<?> getAvatar(Long id) {
		User user = getById(id);
		byte[] data = filesService.downloadAvatar(user.getAvatar(), accessKey);
		return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
	}

	@Transactional
	public void deleteAvatar() {
		User user = getById(getCurrentUser().getId());
		if (user.getAvatar().equals(defaultAvatar)) {
			throw new UserException("You already have default avatar");
		}
		filesService.deleteAvatar(user.getAvatar(), accessKey);
		user.setAvatar(defaultAvatar);
	}

	@Transactional
	public void addComment(Long id, String text) {
		User user = getById(id);
		User currentUser = getCurrentUser();
		if (user.getId().equals(currentUser.getId())) {
			throw new UserException("You cant add comment to yourself");
		}
		commentsRepository.save(Comment.builder()
                .text(text)
                .owner(currentUser)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build());
	}

	@Transactional
	public void deleteComment(Long id) {
		Comment comment = commentsRepository.findById(id).orElseThrow(() -> new UserException("Comment not found"));
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
		gradesRepository.findByUserAndOwner(user, currentUser)
			.ifPresentOrElse(grade -> grade.setStars(stars),
			() -> gradesRepository.save(Grade.builder()
                    .stars(stars)
                    .user(user)
                    .owner(currentUser)
                    .build()));
		double grade = user.getGrades().stream().mapToDouble(Grade::getStars).sum() / user.getGrades().size();
		BigDecimal bigDecimal = BigDecimal.valueOf(grade).setScale(1, RoundingMode.HALF_UP);
		user.setGrade(bigDecimal.doubleValue());
	}

	public List<ResponseReportDTO> reports(int page, int count) {
		Page<Report> data = reportsRepository.findAll(PageRequest.of(page, count));
		return reportMapper.toResponseDTOList(data.getContent());
	}

	@Transactional
	public void report(Long id, String cause) {
		User user = getById(id);
		User currentUser = getCurrentUser();
		reportsRepository.findByUserAndSender(user, currentUser).ifPresent(report -> {
			throw new UserException("You already send report to this user");
		});
		reportsRepository.save(Report.builder()
                .cause(cause)
                .user(user)
                .sender(currentUser)
                .createdAt(LocalDateTime.now())
                .build());
	}

	@Transactional
	public void deleteReport(Integer id) {
		reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
			throw new UserException("Report not found");
		});
	}

	public List<ResponseBannedUserDTO> getBannedUsers(int page, int count) {
		return bannedUsersRepository
				.findAll(PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "id"))).stream()
					.map(bannedUser ->
							new ResponseBannedUserDTO(bannedUser.getUser().getId(), bannedUser.getCause()))
					.toList();
	}

	@Transactional
	public void banUser(Long id, String cause) {
		User user = getById(id);
		bannedUsersRepository.findByUser(user).ifPresent(bannedUser -> {
			throw new UserException("User already banned");
		});
		bannedUsersRepository.save(new BannedUser(cause, user));
		longKafkaTemplate.send("deleted_user_tokens", user.getId());
		String message = "Your account has been banned due to: " + cause;
		emailMessageKafkaTemplate.send("message", new EmailMessageDTO(user.getEmail(), message));
	}

	@Transactional
	public void unbanUser(Long id) {
		User user = getById(id);
		bannedUsersRepository.findByUser(user).ifPresentOrElse(bannedUsersRepository::delete, () -> {
			throw new UserException("User is not banned");
		});
		String message = String.format("Your account with email %s has been unbanned", user.getEmail());
		emailMessageKafkaTemplate.send("message", new EmailMessageDTO(user.getEmail(), message));
	}

	@Transactional
	public void delete() {
		User user = getById(getCurrentUser().getId());
		usersRepository.delete(user);
		if (!user.getAvatar().equals(defaultAvatar)) {
			filesService.deleteAvatar(user.getAvatar(), accessKey);
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
		blockedUsersRepository.findByOwnerAndUser(currentUser, user)
			.ifPresentOrElse(blockedUsersRepository::delete, () -> {
				throw new UserException("User is not blocked");
			});
	}

	public List<DataDTO<Long>> getBlockedUsers(int page, int count) {
		return blockedUsersRepository
				.findAllByOwner(getCurrentUser(), PageRequest.of(page, count,
						Sort.by(Sort.Direction.DESC, "id"))).stream()
					.map(blockedUser -> new DataDTO<>(blockedUser.getUserId()))
					.toList();
	}

	public DataDTO<Boolean> isBlockedByUser(Long id) {
		User user = getById(id);
		return new DataDTO<>(blockedUsersRepository.findByOwnerAndUser(user, getCurrentUser()).isPresent());
	}

	public static User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		return userDetails.user();
	}

}