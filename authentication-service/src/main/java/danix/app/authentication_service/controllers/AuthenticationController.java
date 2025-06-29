package danix.app.authentication_service.controllers;

import danix.app.authentication_service.dto.LoginDTO;
import danix.app.authentication_service.dto.RegistrationDTO;
import danix.app.authentication_service.dto.RegistrationEmailKeyDTO;
import danix.app.authentication_service.dto.ResetPasswordDTO;
import danix.app.authentication_service.dto.TokensDTO;
import danix.app.authentication_service.dto.UpdateEmailDTO;
import danix.app.authentication_service.dto.UpdateEmailKeyDTO;
import danix.app.authentication_service.dto.UpdateUserInfoDTO;
import danix.app.authentication_service.services.AuthenticationService;
import danix.app.authentication_service.util.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Tag(name = "Authentication API")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final EmailKeyValidator emailKeyValidator;

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<TokensDTO> login(@RequestBody @Valid LoginDTO loginDTO, BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        return new ResponseEntity<>(authenticationService.login(loginDTO), HttpStatus.CREATED);
    }

    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> registration(@RequestBody @Valid RegistrationDTO registrationDTO,
                                                   BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        authenticationService.tempRegistration(registrationDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/registration/confirm")
    public ResponseEntity<HttpStatus> registrationConfirm(@RequestBody @Valid RegistrationEmailKeyDTO registrationEmailKeyDTO,
                                                           BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(registrationEmailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        authenticationService.confirmRegistration(registrationEmailKeyDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokensDTO> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            return new ResponseEntity<>(authenticationService.refreshToken(refreshToken), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
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
    public ResponseEntity<HttpStatus> updateEmail(@RequestBody @Valid UpdateEmailKeyDTO emailKeyDTO,
                                                  BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(emailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        authenticationService.updateEmail(emailKeyDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Hidden
    @PutMapping("/user")
    public ResponseEntity<HttpStatus> updateUserInfo(@RequestBody UpdateUserInfoDTO updateUserInfoDTO) {
        authenticationService.updateUserInfo(updateUserInfoDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout(@RequestParam("refresh_token") String refreshToken) {
        authenticationService.logout(refreshToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/disable-user")
    public ResponseEntity<HttpStatus> disableUser(@RequestParam String email) {
        authenticationService.disableUser(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/enable-user")
    public ResponseEntity<HttpStatus> enableUser(@RequestParam String email) {
        authenticationService.enableUser(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Hidden
    @DeleteMapping("/user")
    public ResponseEntity<HttpStatus> deleteUser() {
        authenticationService.deleteUser();
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