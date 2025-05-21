package danix.app.authentication_service.services;

import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.feign.UsersService;
import danix.app.authentication_service.models.EmailKey;
import danix.app.authentication_service.repositories.EmailKeysRepository;
import danix.app.authentication_service.security.User;
import danix.app.authentication_service.models.Token;
import danix.app.authentication_service.repositories.TokensRepository;
import danix.app.authentication_service.security.UserDetailsImpl;
import danix.app.authentication_service.util.AuthenticationException;
import danix.app.authentication_service.util.JWTUtil;
import danix.app.authentication_service.util.TokenData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static danix.app.authentication_service.security.UserDetailsServiceImpl.getCurrentUser;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final KafkaTemplate<String, EmailMessageDTO> kafkaTemplate;

    private final TokensRepository tokensRepository;

    private final JWTUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final UsersService usersService;

    private final PasswordEncoder passwordEncoder;

    private final EmailKeysRepository emailKeysRepository;

    @Value("${access_key}")
    private String accessKey;

    @Value("${email-keys-storage-time-minutes}")
    private int emailKeysStorageTime;

    @Transactional
    public JWTTokenDTO login(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Incorrect password");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.user();
        return new JWTTokenDTO(generateAndGetToken(user.getEmail(), user.getId()));
    }


    public void sendRegistrationKey(RegistrationDTO registrationDTO) {
        checkKeyExists(registrationDTO.getEmail());
        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        usersService.tempRegistration(registrationDTO, accessKey);
        sendKey(registrationDTO.getEmail(), "Your registration code: ");
    }

    @Transactional
    public JWTTokenDTO confirmRegistration(RegistrationEmailKeyDTO emailKeyDTO) {
        String email = emailKeyDTO.getEmail();
        Map<String, Object> response = usersService.registrationConfirm(email, accessKey);
        Long id = Long.parseLong(String.valueOf(response.get("data")));
        emailKeysRepository.deleteById(email);
        return new JWTTokenDTO(generateAndGetToken(email, id));
    }

    public void sendResetPasswordKey(String email) {
        usersService.getUserAuthentication(email, accessKey);
        checkKeyExists(email);
        sendKey(email, "Your key for reset password: ");
    }

    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        String password = passwordEncoder.encode(resetPasswordDTO.getPassword());
        String email = resetPasswordDTO.getEmail();
        usersService.resetPassword(email, password, accessKey);
        emailKeysRepository.deleteById(email);
    }

    public void sendUpdateEmailKey(UpdateEmailDTO updateEmailDTO) {
        String email = updateEmailDTO.getEmail();
        checkKeyExists(email);
        User user = getCurrentUser();
        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Incorrect password");
        }
        sendKey(email, "Your key for update email: ");
    }

    @Transactional
    public JWTTokenDTO updateEmail(UpdateEmailKeyDTO emailKeyDTO, String token) {
        String email = emailKeyDTO.getEmail();
        emailKeysRepository.deleteById(emailKeyDTO.getEmail());
        usersService.updateEmail(email, accessKey, token);
        User user = getCurrentUser();
        tokensRepository.deleteAllByUserId(user.getId());
        return new JWTTokenDTO(generateAndGetToken(email, user.getId()));
    }

    public String validateTokenEndGetEmail(String jwtToken) {
        String id = jwtUtil.getIdFromToken(jwtToken);
        tokensRepository.findById(id).orElseThrow(IllegalStateException::new);
        return jwtUtil.getEmailFromToken(jwtToken);
    }

    @Transactional
    public void deleteUserTokens() {
        tokensRepository.deleteAllByUserId(getCurrentUser().getId());
    }

    @Transactional
    public void deleteExpiredTokens() {
        tokensRepository.deleteAllByExpiredDateBefore(new Date());
    }

    public Optional<EmailKey> getKey(String email) {
        return emailKeysRepository.findById(email);
    }

    @Transactional
    public void incrementEmailKeyAttempts(EmailKey emailKey) {
        emailKey.setAttempts(emailKey.getAttempts() + 1);
        emailKey.setStorageTime(emailKeysStorageTime);
        emailKeysRepository.save(emailKey);
    }

    public void deleteEmailKey(EmailKey emailKey) {
        emailKeysRepository.delete(emailKey);
    }

    private void checkKeyExists(String email) {
        emailKeysRepository.findById(email).ifPresent(emailKey -> {
            throw new AuthenticationException("You already have an active key");
        });
    }

    private String generateAndGetToken(String email, Long userId) {
        TokenData tokenData = jwtUtil.generateToken(email);
        Token token = Token.builder()
                .id(tokenData.id())
                .userId(userId)
                .expiredDate(tokenData.expirationDate())
                .build();
        tokensRepository.save(token);
        return tokenData.token();
    }

    private void sendKey(String email, String message) {
        Random random = new Random();
        int key = random.nextInt(100_000, 999_999);
        EmailKey emailKey = EmailKey.builder()
                .email(email)
                .key(key)
                .storageTime(emailKeysStorageTime)
                .build();
        emailKeysRepository.save(emailKey);
        kafkaTemplate.send("message", new EmailMessageDTO(email, message + key));
    }

    @KafkaListener(topics = {"deleted_user", "deleted_user_tokens"}, containerFactory = "containerFactory")
    @Transactional
    public void deleteUserTokens(Long id) {
        tokensRepository.deleteAllByUserId(id);
    }

}
