package danix.app.authentication_service.controllers;

import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.mapper.UserMapper;
import danix.app.authentication_service.services.AuthenticationService;
import danix.app.authentication_service.util.AuthenticationException;
import danix.app.authentication_service.util.EmailKeyValidator;
import danix.app.authentication_service.util.ErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static danix.app.authentication_service.security.UserDetailsServiceImpl.getCurrentUser;

@Slf4j
@RestController
@Tag(name = "Authentication API")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailKeyValidator emailKeyValidator;

    private final UserMapper userMapper;

    private final AuthenticationService authenticationService;

    @GetMapping("/authorize")
    public ResponseEntity<ResponseUserAuthenticationDTO> authorize() {
        return new ResponseEntity<>(userMapper.toResponseDTO(getCurrentUser()), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<JWTTokenDTO> login(@RequestBody @Valid LoginDTO loginDTO, BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        return new ResponseEntity<>(authenticationService.login(loginDTO), HttpStatus.CREATED);
    }

    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> registration(@RequestBody @Valid RegistrationDTO registrationDTO,
                                                   BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        authenticationService.sendRegistrationKey(registrationDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/registration/confirm")
    public ResponseEntity<JWTTokenDTO> registrationConfirm(@RequestBody @Valid RegistrationEmailKeyDTO registrationEmailKeyDTO,
                                                           BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(registrationEmailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        return new ResponseEntity<>(authenticationService.confirmRegistration(registrationEmailKeyDTO), HttpStatus.OK);
    }

    @PostMapping("/password/reset/key")
    public ResponseEntity<HttpStatus> sendResetPasswordKey(@RequestParam String email) {
        authenticationService.sendResetPasswordKey(email);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<HttpStatus> resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO,
                                                    BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(resetPasswordDTO, bindingResult);
        handleRequestErrors(bindingResult);
        authenticationService.resetPassword(resetPasswordDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/email/update/key")
    public ResponseEntity<HttpStatus> sendUpdateEmailKey(@RequestBody @Valid UpdateEmailDTO updateEmailDTO,
                                                         BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        authenticationService.sendUpdateEmailKey(updateEmailDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/email/update")
    public ResponseEntity<JWTTokenDTO> updateEmail(@RequestHeader("Authorization") String token,
                                                   @RequestBody @Valid UpdateEmailKeyDTO emailKeyDTO,
                                                   BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(emailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        return new ResponseEntity<>(authenticationService.updateEmail(emailKeyDTO, token), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout() {
        authenticationService.deleteUserTokens();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/tokens/expired")
    public ResponseEntity<HttpStatus> deleteExpiredTokens() {
       authenticationService.deleteExpiredTokens();
       return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AuthenticationException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }

    private void handleRequestErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> sb.append(error.getDefaultMessage()).append("; "));
            throw new AuthenticationException(sb.toString());
        }
    }

}