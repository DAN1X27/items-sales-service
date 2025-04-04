package danix.app.users_service.controllers;

import danix.app.users_service.dto.*;
import danix.app.users_service.feignClients.AuthenticationService;
import danix.app.users_service.models.User;
import danix.app.users_service.repositories.BannedUsersRepository;
import danix.app.users_service.services.UserService;
import danix.app.users_service.util.ErrorResponse;
import danix.app.users_service.util.RegistrationValidator;
import danix.app.users_service.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static danix.app.users_service.services.UserService.getCurrentUser;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RegistrationValidator registrationValidator;
    private final BannedUsersRepository bannedUsersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    @Value("${protected_endpoints_key}")
    private String protectedEndpointsKey;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseUserDTO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(userService.show(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/comments")
    public List<ResponseCommentDTO> getUserComments(@PathVariable long id, @RequestParam int page,
                                                    @RequestParam int count) {
        return userService.getUserComments(id, page, count);
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> getUserInfo() {
        return new ResponseEntity<>(userService.userInfo(), HttpStatus.OK);
    }

    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> temporalRegistration(@RequestBody @Valid RegistrationDTO registrationDTO,
                                                           BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        registrationValidator.validate(registrationDTO, bindingResult);
        handleRequestErrors(bindingResult);
        userService.temporalRegistration(registrationDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/registration/confirm")
    public ResponseEntity<Long> registrationConfirm(@RequestParam String email) {
        return new ResponseEntity<>(userService.registrationConfirm(email), HttpStatus.OK);
    }

    @GetMapping("/authentication")
    public ResponseEntity<ResponseUserAuthenticationDTO> getUserAuthentication(@RequestParam String email,
                                                                               @RequestParam String key) {
        if (key == null || !key.equals(protectedEndpointsKey)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        User user = userService.getByEmail(email);
        if (user.getStatus() == User.Status.TEMPORALLY_REGISTERED) {
            throw new UserException("User not found");
        }
        bannedUsersRepository.findByUser(user).ifPresent(bannedUser -> {
            throw new UserException("Account was blocked due to: " + bannedUser.getCause());
        });
        return new ResponseEntity<>(
                ResponseUserAuthenticationDTO.builder()
                        .email(user.getEmail())
                        .id(user.getId())
                        .role(user.getRole())
                        .password(user.getPassword())
                        .city(user.getCity())
                        .country(user.getCountry())
                        .build(),
                HttpStatus.OK
        );
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> delete() {
        userService.delete();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping
    public ResponseEntity<HttpStatus> updateInfo(@RequestBody UpdateInfoDTO updateInfoDTO) {
        userService.updatePassword(updateInfoDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/password")
    public ResponseEntity<HttpStatus> resetPassword(@RequestParam String email, @RequestParam String password,
                                                    @RequestParam String key) {
        if (key == null || !key.equals(protectedEndpointsKey)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        User user = userService.getByEmail(email);
        userService.updatePassword(user, password);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/password")
    public ResponseEntity<HttpStatus> updatePassword(@RequestBody @Valid UpdatePasswordDTO updatePasswordDTO,
                                                     BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        User user = userService.getById(getCurrentUser().getId());
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new UserException("Incorrect password");
        }
        if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), user.getPassword())) {
            throw new UserException("The new password must be different from the old one");
        }
        userService.updatePassword(user, passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/email")
    public ResponseEntity<HttpStatus> sendUpdateEmailKey(@RequestHeader("Authorization") String token,
                                                         @RequestBody @Valid UpdateEmailDTO updateEmailDTO,
                                                         BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        User user = getCurrentUser();
        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
            throw new UserException("Incorrect password");
        }
        String message = "Your key for update email: ";
        authenticationService.sendEmailKey(updateEmailDTO.getEmail(), message, token);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/email")
    public ResponseEntity<Map<String, Object>> updateEmail(@RequestBody Map<String, Object> emailKey,
                                                           @RequestHeader("Authorization") String token) {
        Map<String, Object> jwtToken = authenticationService.validateEmailKey(emailKey, token);
        String email = (String) emailKey.get("email");
        userService.updateEmail(email);
        return new ResponseEntity<>(jwtToken, HttpStatus.OK);
    }

    @PatchMapping("/avatar")
    public ResponseEntity<HttpStatus> updateAvatar(@RequestParam MultipartFile image) {
        userService.updateAvatar(image);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/avatar")
    public ResponseEntity<?> downloadAvatar() {
        ResponseAvatarDTO response = parseAvatar(userService.downloadAvatar(getCurrentUser().getId()));
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(response.getMediaType())
                .body(response.getData());
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<?> downloadUserAvatar(@PathVariable Long id) {
        ResponseAvatarDTO response = parseAvatar(userService.downloadAvatar(id));
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(response.getMediaType())
                .body(response.getData());
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<HttpStatus> deleteAvatar() {
        userService.deleteAvatar();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<HttpStatus> createComment(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String comment = (String) data.get("comment");
        if (comment == null) {
            throw new UserException("Comment must not be empty");
        }
        userService.addComment(id, comment);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/comment/{id}")
    public ResponseEntity<HttpStatus> deleteComment(@PathVariable Long id) {
        userService.deleteComment(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/grade")
    public ResponseEntity<HttpStatus> addGrade(@PathVariable Long id, @RequestParam int stars) {
        userService.addGrade(id, stars);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ResponseReportDTO>> reports(@RequestParam int page, @RequestParam int count) {
        return new ResponseEntity<>(userService.reports(page, count), HttpStatus.OK);
    }

    @DeleteMapping("/report/{id}")
    public ResponseEntity<HttpStatus> deleteReport(@PathVariable Integer id) {
        userService.deleteReport(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<HttpStatus> report(@PathVariable Long id, @RequestParam String cause) {
        userService.report(id, cause);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<HttpStatus> ban(@PathVariable Long id, @RequestParam String cause,
                                          @RequestHeader("Authorization") String token) {
        userService.banUser(id, cause, token);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/unban")
    public ResponseEntity<HttpStatus> unban(@PathVariable Long id) {
        userService.unbanUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<HttpStatus> blockUser(@PathVariable Long id) {
        userService.blockUser(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/unblock")
    public ResponseEntity<HttpStatus> unblockUser(@PathVariable Long id) {
        userService.unblockUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/blocked-users")
    public ResponseEntity<List<ResponseBlockedUserDTO>> getBlockedUsers() {
        return new ResponseEntity<>(userService.getBlockedUsers(), HttpStatus.OK);
    }

    @GetMapping("/{id}/is-blocked")
    public boolean checkIsBlocked(@PathVariable Long id) {
        return userService.isUserBlocked(id);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(UserException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }

    private ResponseAvatarDTO parseAvatar(Map<String, Object> data) {
        byte[] bytes = Base64.getDecoder().decode((String) data.get("data"));
        MediaType mediaType = MediaType.parseMediaType((String) data.get("mediaType"));
        return ResponseAvatarDTO.builder()
                .data(bytes)
                .mediaType(mediaType)
                .build();
    }

    private void handleRequestErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder message = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error ->
                    message.append(error.getDefaultMessage()).append("; "));
            throw new UserException(message.toString());
        }
    }
}