package danix.app.users_service.controllers;

import danix.app.users_service.dto.*;
import danix.app.users_service.util.SecurityUtil;
import danix.app.users_service.services.UsersService;
import danix.app.users_service.util.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Users API")
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

	private final UsersService usersService;

	private final RegistrationValidator registrationValidator;

	private final UpdateInfoValidator updateInfoValidator;

	private final SecurityUtil securityUtil;

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
		return new ResponseEntity<>(usersService.getInfo(), HttpStatus.OK);
	}

	@Hidden
	@GetMapping("/{id}/email")
	public ResponseEntity<DataDTO<String>> getUserEmail(@PathVariable long id) {
		return new ResponseEntity<>(new DataDTO<>(usersService.getById(id).getEmail()), HttpStatus.OK);
	}

	@Hidden
	@PostMapping("/registration")
	public ResponseEntity<HttpStatus> temporalRegistration(@RequestBody RegistrationDTO registrationDTO,
														   BindingResult bindingResult) {
		registrationValidator.validate(registrationDTO, bindingResult);
		handleRequestErrors(bindingResult);
		usersService.temporalRegistration(registrationDTO);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@Hidden
	@PatchMapping("/registration/confirm")
	public ResponseEntity<DataDTO<Long>> registrationConfirm(@RequestParam String email) {
		return new ResponseEntity<>(usersService.registrationConfirm(email), HttpStatus.OK);
	}

	@DeleteMapping
	public ResponseEntity<HttpStatus> delete() {
		usersService.delete();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Hidden
	@DeleteMapping("/{id}")
	public ResponseEntity<HttpStatus> delete(@PathVariable Long id) {
		usersService.delete(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping
	public ResponseEntity<HttpStatus> updateInfo(@RequestBody @Valid UpdateInfoDTO updateInfoDTO,
												 BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		updateInfoValidator.validate(updateInfoDTO, bindingResult);
		handleRequestErrors(bindingResult);
		usersService.updateInfo(updateInfoDTO);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Hidden
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
		return usersService.getAvatar(securityUtil.getCurrentUser().getId());
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
		return new ResponseEntity<>(usersService.getReports(page, count), HttpStatus.OK);
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
		usersService.createReport(id, causeDTO.getCause());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping("/banned")
	public ResponseEntity<List<ResponseBannedUserDTO>> getBannedUsers(@RequestParam int page, @RequestParam int count) {
		return new ResponseEntity<>(usersService.getBannedUsers(page, count), HttpStatus.OK);
	}

	@Hidden
	@GetMapping("is-banned")
	public ResponseEntity<Map<String, Object>> isUserBanned(@RequestParam String username) {
		return new ResponseEntity<>(usersService.isBanned(username), HttpStatus.OK);
	}

	@PostMapping("/{id}/ban")
	public ResponseEntity<HttpStatus> ban(@PathVariable Long id, @RequestBody @Valid CauseDTO causeDTO,
			 BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		usersService.banUser(id, causeDTO.getCause());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PatchMapping("/{id}/unban")
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

	@Hidden
	@DeleteMapping("/temp")
	public ResponseEntity<HttpStatus> deleteTempUser(@RequestParam String email) {
		usersService.deleteTempUser(email);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void handleRequestErrors(BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			LocalDateTime timestamp = LocalDateTime.now();
			Map<String, ErrorData> error = bindingResult.getFieldErrors().stream()
					.collect(Collectors.toMap(FieldError::getField, fieldError -> new ErrorData(
							fieldError.getDefaultMessage(), timestamp)));
			throw new RequestException(error);
		}
	}

}