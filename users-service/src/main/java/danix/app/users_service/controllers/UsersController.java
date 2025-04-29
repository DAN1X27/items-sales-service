package danix.app.users_service.controllers;

import danix.app.users_service.dto.*;
import danix.app.users_service.models.User;
import danix.app.users_service.services.UsersService;
import danix.app.users_service.util.ErrorResponse;
import danix.app.users_service.util.RegistrationValidator;
import danix.app.users_service.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static danix.app.users_service.services.UsersService.getCurrentUser;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

	private final UsersService usersService;

	private final RegistrationValidator registrationValidator;

	private final PasswordEncoder passwordEncoder;

	@GetMapping("/{id}")
	public ResponseEntity<ResponseUserDTO> findById(@PathVariable Long id) {
		return new ResponseEntity<>(usersService.show(id), HttpStatus.OK);
	}

	@GetMapping("/{id}/comments")
	public ResponseEntity<List<ResponseCommentDTO>> getUserComments(@PathVariable long id, @RequestParam int page,
			@RequestParam int count) {
		return new ResponseEntity<>(usersService.getUserComments(id, page, count), HttpStatus.OK);
	}

	@GetMapping("/info")
	public ResponseEntity<UserInfoDTO> getUserInfo() {
		return new ResponseEntity<>(usersService.userInfo(), HttpStatus.OK);
	}

	@GetMapping("/{id}/email")
	public ResponseEntity<DataDTO<String>> getUserEmail(@PathVariable long id) {
		return new ResponseEntity<>(new DataDTO<>(usersService.getById(id).getEmail()), HttpStatus.OK);
	}

	@PostMapping("/registration")
	public ResponseEntity<HttpStatus> temporalRegistration(@RequestBody RegistrationDTO registrationDTO,
				BindingResult bindingResult) {
		registrationValidator.validate(registrationDTO, bindingResult);
		handleRequestErrors(bindingResult);
		usersService.temporalRegistration(registrationDTO);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PatchMapping("/registration/confirm")
	public ResponseEntity<DataDTO<Long>> registrationConfirm(@RequestParam String email) {
		return new ResponseEntity<>(usersService.registrationConfirm(email), HttpStatus.OK);
	}

	@GetMapping("/authentication")
	public ResponseEntity<AuthenticationDTO> getUserAuthentication(@RequestParam String email) {
		return new ResponseEntity<>(usersService.getAuthentication(email), HttpStatus.OK);
	}

	@DeleteMapping
	public ResponseEntity<HttpStatus> delete() {
		usersService.delete();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping
	public ResponseEntity<HttpStatus> updateInfo(@RequestBody UpdateInfoDTO updateInfoDTO) {
		usersService.updateInfo(updateInfoDTO);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping("/password/reset")
	public ResponseEntity<HttpStatus> resetPassword(@RequestParam String email, @RequestParam String password) {
		User user = usersService.getByEmail(email);
		usersService.updatePassword(user, password);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping("/password")
	public ResponseEntity<HttpStatus> updatePassword(@RequestBody @Valid UpdatePasswordDTO updatePasswordDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		User user = usersService.getById(getCurrentUser().getId());
		if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
			throw new UserException("Incorrect password");
		}
		if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), user.getPassword())) {
			throw new UserException("The new password must be different from the old one");
		}
		usersService.updatePassword(user, passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping("/email")
	public ResponseEntity<HttpStatus> updateEmail(@RequestParam String email) {
		usersService.updateEmail(email);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/avatar")
	public ResponseEntity<HttpStatus> updateAvatar(@RequestParam MultipartFile image) {
		usersService.updateAvatar(image);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/avatar")
	public ResponseEntity<?> downloadAvatar() {
		return usersService.getAvatar(getCurrentUser().getId());
	}

	@GetMapping("/{id}/avatar")
	public ResponseEntity<?> downloadUserAvatar(@PathVariable Long id) {
		return usersService.getAvatar(id);
	}

	@DeleteMapping("/avatar")
	public ResponseEntity<HttpStatus> deleteAvatar() {
		usersService.deleteAvatar();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/comment")
	public ResponseEntity<HttpStatus> createComment(@PathVariable Long id, @RequestBody Map<String, Object> data) {
		String comment = (String) data.get("comment");
		if (comment == null || comment.isBlank()) {
			throw new UserException("Comment must not be empty");
		}
		usersService.addComment(id, comment);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@DeleteMapping("/comment/{id}")
	public ResponseEntity<HttpStatus> deleteComment(@PathVariable Long id) {
		usersService.deleteComment(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/grade")
	public ResponseEntity<HttpStatus> addGrade(@PathVariable Long id, @RequestParam int stars) {
		usersService.addGrade(id, stars);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping("/reports")
	public ResponseEntity<List<ResponseReportDTO>> reports(@RequestParam int page, @RequestParam int count) {
		return new ResponseEntity<>(usersService.reports(page, count), HttpStatus.OK);
	}

	@DeleteMapping("/report/{id}")
	public ResponseEntity<HttpStatus> deleteReport(@PathVariable Integer id) {
		usersService.deleteReport(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/report")
	public ResponseEntity<HttpStatus> report(@PathVariable Long id, @RequestBody @Valid CauseDTO causeDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		usersService.report(id, causeDTO.getCause());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping("/banned")
	public ResponseEntity<List<ResponseBannedUserDTO>> getBannedUsers(@RequestParam int page, @RequestParam int count) {
		return new ResponseEntity<>(usersService.getBannedUsers(page, count), HttpStatus.OK);
	}

	@PostMapping("/{id}/ban")
	public ResponseEntity<HttpStatus> ban(@PathVariable Long id, @RequestBody @Valid CauseDTO causeDTO,
			 BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		usersService.banUser(id, causeDTO.getCause());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}/unban")
	public ResponseEntity<HttpStatus> unban(@PathVariable Long id) {
		usersService.unbanUser(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/block")
	public ResponseEntity<HttpStatus> blockUser(@PathVariable Long id) {
		usersService.blockUser(id);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}/unblock")
	public ResponseEntity<HttpStatus> unblockUser(@PathVariable Long id) {
		usersService.unblockUser(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/blocked")
	public ResponseEntity<List<DataDTO<Long>>> getBlockedUsers(@RequestParam int page, @RequestParam int count) {
		return new ResponseEntity<>(usersService.getBlockedUsers(page, count), HttpStatus.OK);
	}

	@GetMapping("/{id}/is-blocked")
	public ResponseEntity<DataDTO<Boolean>> checkIsBlocked(@PathVariable Long id) {
		return new ResponseEntity<>(usersService.isBlockedByUser(id), HttpStatus.OK);
	}

	@DeleteMapping("/temp")
	public ResponseEntity<HttpStatus> deleteTempUsers() {
		usersService.deleteTempUsers();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(UserException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

	private void handleRequestErrors(BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			StringBuilder message = new StringBuilder();
			bindingResult.getFieldErrors().forEach(error -> message.append(error.getDefaultMessage()).append("; "));
			throw new UserException(message.toString());
		}
	}

}