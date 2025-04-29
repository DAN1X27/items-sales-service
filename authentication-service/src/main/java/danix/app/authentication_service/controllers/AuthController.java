package danix.app.authentication_service.controllers;

import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.mapper.UserMapper;
import danix.app.authentication_service.security.User;
import danix.app.authentication_service.security.UserDetailsImpl;
import danix.app.authentication_service.services.EmailKeysService;
import danix.app.authentication_service.services.TokensService;
import danix.app.authentication_service.feign.UsersService;
import danix.app.authentication_service.util.AuthenticationException;
import danix.app.authentication_service.util.EmailKeyValidator;
import danix.app.authentication_service.util.ErrorResponse;
import danix.app.authentication_service.util.JWTUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final JWTUtil jwtUtil;

	private final AuthenticationProvider authenticationProvider;

	private final UsersService usersService;

	private final PasswordEncoder passwordEncoder;

	private final TokensService tokensService;

	private final EmailKeysService emailKeysService;

	private final EmailKeyValidator emailKeyValidator;

	private final UserMapper userMapper;

	@Value("${access_key}")
	private String accessKey;

	@GetMapping("/authorize")
	public ResponseEntity<ResponseUserAuthenticationDTO> authorize() {
		return new ResponseEntity<>(userMapper.toResponseDTO(getCurrentUser()), HttpStatus.OK);
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid LoginDTO loginDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		UsernamePasswordAuthenticationToken authToken =
				new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());
		Authentication authentication;
		try {
			authentication = authenticationProvider.authenticate(authToken);
		}
		catch (BadCredentialsException e) {
			throw new AuthenticationException("Incorrect password");
		}
		UserDetailsImpl details = (UserDetailsImpl) authentication.getPrincipal();
		String token = jwtUtil.generateToken(loginDTO.getEmail());
		tokensService.save(token, details.user().getId());
		return new ResponseEntity<>(Map.of("jwt-token", token), HttpStatus.CREATED);
	}

	@PostMapping("/registration")
	public ResponseEntity<HttpStatus> registration(@RequestBody @Valid RegistrationDTO registrationDTO,
				BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		emailKeysService.getByEmail(registrationDTO.getEmail()).ifPresent(key -> {
			throw new AuthenticationException("You already have an active key");
		});
		registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
		usersService.tempRegistration(registrationDTO, accessKey);
		emailKeysService.sendKey(registrationDTO.getEmail(), "Your registration key: ");
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/registration/confirm")
	public ResponseEntity<Map<String, Object>> registrationConfirm(
			@RequestBody @Valid EmailKeyDTO registrationEmailKeyDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		emailKeyValidator.validate(registrationEmailKeyDTO, bindingResult);
		handleRequestErrors(bindingResult);
		String email = registrationEmailKeyDTO.getEmail();
		Map<String, Object> data = usersService.registrationConfirm(email, accessKey);
		Long id = Long.parseLong(String.valueOf(data.get("data")));
		emailKeysService.delete(email);
		String token = jwtUtil.generateToken(email);
		tokensService.save(token, id);
		return new ResponseEntity<>(Map.of("jwt-token", token), HttpStatus.OK);
	}

	@PostMapping("/password/reset/key")
	public ResponseEntity<HttpStatus> sendResetPasswordKey(@RequestParam String email) {
		usersService.getUserAuthentication(email, accessKey);
		emailKeysService.getByEmail(email).ifPresent(key -> {
			throw new AuthenticationException("You already have an active key");
		});
		emailKeysService.sendKey(email, "Your reset password key: ");
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PatchMapping("/password/reset")
	public ResponseEntity<HttpStatus> resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		emailKeyValidator.validate(resetPasswordDTO, bindingResult);
		handleRequestErrors(bindingResult);
		String password = passwordEncoder.encode(resetPasswordDTO.getPassword());
		usersService.resetPassword(resetPasswordDTO.getEmail(), password, accessKey);
		emailKeysService.delete(resetPasswordDTO.getEmail());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/email/update/key")
	public ResponseEntity<HttpStatus> sendUpdateEmailKey(@RequestBody @Valid UpdateEmailDTO updateEmailDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		emailKeysService.getByEmail(updateEmailDTO.getEmail()).ifPresent(key -> {
			throw new AuthenticationException("You already have an active key");
		});
		User user = getCurrentUser();
		if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
			throw new AuthenticationException("Incorrect password");
		}
		emailKeysService.sendKey(updateEmailDTO.getEmail(), "Your key for update email: ");
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PatchMapping("/email/update")
	public ResponseEntity<Map<String, Object>> validateUpdateEmailKey(@RequestHeader("Authorization") String token,
			@RequestBody @Valid EmailKeyDTO emailKeyDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		emailKeyValidator.validate(emailKeyDTO, bindingResult);
		handleRequestErrors(bindingResult);
		emailKeysService.delete(emailKeyDTO.getEmail());
		User user = getCurrentUser();
		usersService.updateEmail(emailKeyDTO.getEmail(), accessKey, token);
		tokensService.deleteUserTokens(user.getId());
		String jwtToken = jwtUtil.generateToken(emailKeyDTO.getEmail());
		tokensService.save(jwtToken, user.getId());
		return new ResponseEntity<>(Map.of("jwt-token", jwtToken), HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<HttpStatus> logout() {
		tokensService.deleteUserTokens(getCurrentUser().getId());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/tokens/expired")
	public void deleteExpiredTokens() {
		tokensService.deleteExpiredTokens();
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(AuthenticationException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

	@KafkaListener(topics = {"deleted_user", "deleted_user_tokens"}, containerFactory = "containerFactory")
	public void deleteUserTokens(Long id) {
		tokensService.deleteUserTokens(id);
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		return userDetails.user();
	}

	private void handleRequestErrors(BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			StringBuilder sb = new StringBuilder();
			bindingResult.getFieldErrors().forEach(error -> sb.append(error.getDefaultMessage()).append("; "));
			throw new AuthenticationException(sb.toString());
		}
	}

}