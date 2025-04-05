package danix.app.authentication_service.controllers;

import danix.app.authentication_service.dto.LoginDTO;
import danix.app.authentication_service.dto.RegistrationDTO;
import danix.app.authentication_service.dto.EmailKeyDTO;
import danix.app.authentication_service.dto.ResetPasswordDTO;
import danix.app.authentication_service.security.UserDetailsImpl;
import danix.app.authentication_service.services.EmailKeyService;
import danix.app.authentication_service.services.TokenService;
import danix.app.authentication_service.feign_clients.UserService;
import danix.app.authentication_service.util.AuthenticationException;
import danix.app.authentication_service.util.EmailKeyValidator;
import danix.app.authentication_service.util.ErrorResponse;
import danix.app.authentication_service.util.JWTUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JWTUtil jwtUtil;
    private final AuthenticationProvider authenticationProvider;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailKeyService emailKeyService;
    private final EmailKeyValidator emailKeyValidator;
    @Value("${protected_endpoints_key}")
    private String protectedEndpointsKey;

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, Object>> authorize(@RequestHeader("Authorization") String header) {
        if (!header.startsWith("Bearer ")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String token = header.substring(7);
        try {
            return new ResponseEntity<>(userService.getUserAuthentication(tokenService.validateToken(token),
                    protectedEndpointsKey), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
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
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Incorrect password");
        }
        UserDetailsImpl details = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtil.generateToken(loginDTO.getEmail());
        tokenService.save(token, details.userAuthentication().getId());
        return new ResponseEntity<>(Map.of("jwt-token", token), HttpStatus.CREATED);
    }

    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> registration(@RequestBody RegistrationDTO registrationDTO) {
        emailKeyService.getByEmail(registrationDTO.getEmail()).ifPresent(key -> {
            throw new AuthenticationException("You already have an active key");
        });
        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        userService.tempRegistration(registrationDTO);
        emailKeyService.registrationKey(registrationDTO.getEmail());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/password/reset/key")
    public ResponseEntity<HttpStatus> sendResetPasswordKey(@RequestParam String email) {
        userService.getUserAuthentication(email, protectedEndpointsKey);
        emailKeyService.getByEmail(email).ifPresent(key -> {
            throw new AuthenticationException("You already have an active key");
        });
        emailKeyService.resetPasswordKey(email);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<HttpStatus> resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO,
                                                    BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(resetPasswordDTO, bindingResult);
        handleRequestErrors(bindingResult);
        String password = passwordEncoder.encode(resetPasswordDTO.getPassword());
        userService.resetPassword(resetPasswordDTO.getEmail(), password, protectedEndpointsKey);
        emailKeyService.delete(resetPasswordDTO.getEmail());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/key")
    public ResponseEntity<HttpStatus> sendEmailKey(@RequestParam String email, @RequestParam String message) {
        emailKeyService.getByEmail(email).ifPresent(key -> {
            throw new AuthenticationException("You already have an active key");
        });
        emailKeyService.sendKey(email, message);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/email/update")
    public ResponseEntity<Map<String, Object>> validateUpdateEmailKey(@RequestBody @Valid EmailKeyDTO registrationEmailKeyDTO,
                                                                      BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(registrationEmailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        emailKeyService.delete(registrationEmailKeyDTO.getEmail());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        tokenService.deleteUserTokens(userDetails.userAuthentication().getId());
        String token = jwtUtil.generateToken(registrationEmailKeyDTO.getEmail());
        tokenService.save(token, userDetails.userAuthentication().getId());
        return new ResponseEntity<>(Map.of("jwt-token", token), HttpStatus.OK);
    }

    @PostMapping("/registration-confirm")
    public ResponseEntity<Map<String, Object>> registrationConfirm(@RequestBody @Valid EmailKeyDTO registrationEmailKeyDTO,
                                                                   BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        emailKeyValidator.validate(registrationEmailKeyDTO, bindingResult);
        handleRequestErrors(bindingResult);
        String email = registrationEmailKeyDTO.getEmail();
        emailKeyService.delete(email);
        Long id = userService.registrationConfirm(email);
        String token = jwtUtil.generateToken(email);
        tokenService.save(token, id);
        return new ResponseEntity<>(Map.of("jwt-token", token), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> data = userService.getUserInfo(token);
            Long id = Long.parseLong(String.valueOf(data.get("id")));
            tokenService.deleteUserTokens(id);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/tokens/{id}")
    public ResponseEntity<HttpStatus> deleteUserTokens(@PathVariable Long id) {
        tokenService.deleteUserTokens(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AuthenticationException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }

    private void handleRequestErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error ->
                    sb.append(error.getDefaultMessage()).append("; "));
            throw new AuthenticationException(sb.toString());
        }
    }
}