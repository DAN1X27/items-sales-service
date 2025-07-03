package danix.app.authentication_service.services.impl;

import danix.app.authentication_service.config.KeycloakProperties;
import danix.app.authentication_service.dto.EmailMessageDTO;
import danix.app.authentication_service.dto.LoginDTO;
import danix.app.authentication_service.dto.RegistrationDTO;
import danix.app.authentication_service.dto.RegistrationEmailKeyDTO;
import danix.app.authentication_service.dto.ResetPasswordDTO;
import danix.app.authentication_service.dto.TempRegistrationDTO;
import danix.app.authentication_service.dto.TokensDTO;
import danix.app.authentication_service.dto.UpdateEmailDTO;
import danix.app.authentication_service.dto.UpdateEmailKeyDTO;
import danix.app.authentication_service.dto.UpdateUserInfoDTO;
import danix.app.authentication_service.feign.KeycloakAPI;
import danix.app.authentication_service.feign.UsersAPI;
import danix.app.authentication_service.keycloak_dto.UserAttributesDTO;
import danix.app.authentication_service.keycloak_dto.CredentialsDTO;
import danix.app.authentication_service.keycloak_dto.KeycloakRegistrationDTO;
import danix.app.authentication_service.keycloak_dto.UserInfoDTO;
import danix.app.authentication_service.mapper.RegistrationMapper;
import danix.app.authentication_service.models.EmailKey;
import danix.app.authentication_service.repositories.EmailKeysRepository;
import danix.app.authentication_service.services.AuthenticationService;
import danix.app.authentication_service.util.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final KafkaTemplate<String, EmailMessageDTO> kafkaTemplate;

    private final UsersAPI usersAPI;

    private final EmailKeysRepository emailKeysRepository;

    private final KeycloakProperties keycloakProperties;

    private final KeycloakAPI keycloakAPI;

    private final RegistrationMapper registrationMapper;

    private static final Random random = new Random();

    @Value("${access_key}")
    private String accessKey;

    @Value("${email-keys-storage-time-minutes}")
    private int emailKeysStorageTime;

    @Override
    public TokensDTO login(LoginDTO loginDTO) {
        Map<String, Object> isBannedResponse = usersAPI.isUserBanned(loginDTO.getUsername(), accessKey);
        if ((boolean) isBannedResponse.get("is_banned")) {
            String cause = (String) isBannedResponse.get("cause");
            throw new AuthenticationException("Your account has been blocked due to: " + cause);
        }
        MultiValueMap<String, Object> loginData = new LinkedMultiValueMap<>();
        loginData.add("grant_type", "password");
        loginData.add("client_id", keycloakProperties.getClientId());
        loginData.add("client_secret", keycloakProperties.getClientSecret());
        loginData.add("username", loginDTO.getUsername());
        loginData.add("password", loginDTO.getPassword());
        Map<String, String> tokens;
        try {
            tokens = getTokens(loginData);
        } catch (Exception e) {
            throw new AuthenticationException("Incorrect username or password");
        }
        return new TokensDTO(
                Objects.requireNonNull(tokens.get("access_token")),
                Objects.requireNonNull(tokens.get("refresh_token"))
        );
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, Object> logoutData = new LinkedMultiValueMap<>();
        logoutData.add("refresh_token", refreshToken);
        logoutData.add("client_id", keycloakProperties.getClientId());
        logoutData.add("client_secret", keycloakProperties.getClientSecret());
        keycloakAPI.logout(logoutData, getAccessToken());
    }

    @Override
    public void disableUser(String email) {
        UserInfoDTO userInfo = getUserInfo(email);
        userInfo.setEnabled(false);
        keycloakAPI.updateUserInfo(userInfo.getId(), userInfo, getAdminAccessToken());
    }

    @Override
    public void enableUser(String email) {
        UserInfoDTO userInfo = getUserInfo(email);
        userInfo.setEnabled(true);
        keycloakAPI.updateUserInfo(userInfo.getId(), userInfo, getAdminAccessToken());
    }

    @Override
    public void tempRegistration(RegistrationDTO registrationDTO) {
        checkKeyExists(registrationDTO.getEmail());
        TempRegistrationDTO tempRegistrationDTO = registrationMapper.toTempRegistrationDTO(registrationDTO);
        usersAPI.tempRegistration(tempRegistrationDTO, accessKey);
        CredentialsDTO credentials = CredentialsDTO.builder()
                .type("password")
                .temporary(false)
                .value(registrationDTO.getPassword())
                .build();
        UserAttributesDTO attributes = registrationMapper.toAttributes(registrationDTO);
        KeycloakRegistrationDTO registration = registrationMapper
                .toRegistration(registrationDTO, attributes, List.of(credentials));
        keycloakAPI.registration(registration, getAdminAccessToken());
        sendEmailKey(registrationDTO.getEmail(), "Your registration key: ");
    }

    @Override
    public void confirmRegistration(RegistrationEmailKeyDTO emailKeyDTO) {
        String email = emailKeyDTO.getEmail();
        Map<String, Long> idData = usersAPI.registrationConfirm(email, accessKey);
        Long id = idData.get("data");
        UserInfoDTO userInfo = null;
        try {
            userInfo = getUserInfo(email);
            userInfo.setEnabled(true);
            UserAttributesDTO userAttributes = userInfo.getAttributes();
            userAttributes.setId(List.of(id));
            String userId = userInfo.getId();
            keycloakAPI.updateUserInfo(userId, userInfo, getAdminAccessToken());
        } catch (Exception e) {
            usersAPI.deleteUser(id, accessKey);
            if (userInfo != null) {
                keycloakAPI.deleteUser(userInfo.getId(), getAdminAccessToken());
            }
            throw e;
        } finally {
            emailKeysRepository.deleteById(email);
        }
    }

    @Override
    public TokensDTO refreshToken(String refreshToken) {
        MultiValueMap<String, Object> refreshTokenData = new LinkedMultiValueMap<>();
        refreshTokenData.add("grant_type", "refresh_token");
        refreshTokenData.add("refresh_token", refreshToken);
        refreshTokenData.add("client_id", keycloakProperties.getClientId());
        refreshTokenData.add("client_secret", keycloakProperties.getClientSecret());
        Map<String, String> tokens = getTokens(refreshTokenData);
        return new TokensDTO(
                Objects.requireNonNull(tokens.get("access_token")),
                Objects.requireNonNull(tokens.get("refresh_token"))
        );
    }

    @Override
    public void sendResetPasswordKey(String email) {
        getUserInfo(email);
        checkKeyExists(email);
        sendEmailKey(email, "Your key for reset password: ");
    }

    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        emailKeysRepository.deleteById(resetPasswordDTO.getEmail());
        UserInfoDTO userInfo = getUserInfo(resetPasswordDTO.getEmail());
        String userId = userInfo.getId();
        Map<String, Object> resetPasswordData = new HashMap<>();
        resetPasswordData.put("type", "password");
        resetPasswordData.put("temporary", false);
        resetPasswordData.put("value", resetPasswordDTO.getPassword());
        keycloakAPI.resetPassword(userId, resetPasswordData, getAdminAccessToken());
    }

    private UserInfoDTO getUserInfo(String email) {
        List<UserInfoDTO> response = keycloakAPI.getUser(email, getAdminAccessToken());
        if (response.isEmpty()) {
            throw new AuthenticationException("User not found");
        }
        return response.getFirst();
    }

    @Override
    public void sendUpdateEmailKey(UpdateEmailDTO updateEmailDTO) {
        String email = updateEmailDTO.getEmail();
        checkKeyExists(email);
        sendEmailKey(email, "Your key for update email: ");
    }

    @Override
    public void updateEmail(UpdateEmailKeyDTO emailKeyDTO) {
        String updatedEmail = emailKeyDTO.getEmail();
        emailKeysRepository.deleteById(emailKeyDTO.getEmail());
        usersAPI.updateEmail(updatedEmail, accessKey, getAccessToken());
        String email = getJwt().getClaim("email");
        try {
            UserInfoDTO userInfo = getUserInfo(email);
            userInfo.setEmail(updatedEmail);
            keycloakAPI.updateUserInfo(getUserIdFromJwt(), userInfo, getAdminAccessToken());
        } catch (Exception e) {
            usersAPI.updateEmail(email, accessKey, getAccessToken());
            throw e;
        }
    }

    @Override
    public void updateUserInfo(UpdateUserInfoDTO updateUserInfoDTO) {
        Jwt jwt = getJwt();
        String email = jwt.getClaim("email");
        UserInfoDTO userInfo = getUserInfo(email);
        UserAttributesDTO attributes = userInfo.getAttributes();
        if (updateUserInfoDTO.getUsername() != null) {
            userInfo.setUsername(updateUserInfoDTO.getUsername());
        }
        if (updateUserInfoDTO.getFirstName() != null) {
            attributes.setFirstName(List.of(updateUserInfoDTO.getFirstName()));
        }
        if (updateUserInfoDTO.getLastName() != null) {
            attributes.setLastName(List.of(updateUserInfoDTO.getLastName()));
        }
        if (updateUserInfoDTO.getCountry() != null) {
            attributes.setCountry(List.of(updateUserInfoDTO.getCountry()));
        }
        if (updateUserInfoDTO.getCity() != null) {
            attributes.setCity(List.of(updateUserInfoDTO.getCity()));
        }
        keycloakAPI.updateUserInfo(getUserIdFromJwt(), userInfo, getAdminAccessToken());
    }

    @Override
    public void deleteUser() {
        keycloakAPI.deleteUser(getUserIdFromJwt(), getAdminAccessToken());
    }

    @Override
    public void deleteUser(String email) {
        UserInfoDTO userInfo = getUserInfo(email);
        keycloakAPI.deleteUser(userInfo.getId(), getAdminAccessToken());
    }

    private String getUserIdFromJwt() {
        return getJwt().getClaim(JwtClaimNames.SUB);
    }

    private Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Jwt) authentication.getPrincipal();
    }

    @Override
    public Optional<EmailKey> getKey(String email) {
        return emailKeysRepository.findById(email);
    }

    @Override
    public void incrementEmailKeyAttempts(EmailKey emailKey) {
        emailKey.setAttempts(emailKey.getAttempts() + 1);
        emailKey.setStorageTime(emailKeysStorageTime);
        emailKeysRepository.save(emailKey);
    }

    @Override
    public void deleteEmailKey(EmailKey emailKey) {
        emailKeysRepository.delete(emailKey);
    }

    private void checkKeyExists(String email) {
        emailKeysRepository.findById(email).ifPresent(emailKey -> {
            throw new AuthenticationException("You already have an active key");
        });
    }

    private String getAccessToken() {
        return "Bearer " + getJwt().getTokenValue();
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, Object> adminTokenData = new LinkedMultiValueMap<>();
        adminTokenData.add("grant_type", "client_credentials");
        adminTokenData.add("client_id", keycloakProperties.getAdminClientId());
        adminTokenData.add("client_secret", keycloakProperties.getAdminClientSecret());
        Map<String, String> tokens = getTokens(adminTokenData);
        return "Bearer " + Objects.requireNonNull(tokens.get("access_token"));
    }

    private Map<String, String> getTokens(MultiValueMap<String, Object> requestBody) {
        Map<String, Object> response = keycloakAPI.getTokens(requestBody);
        Map<String, String> tokens = new HashMap<>();
        if (response.get("access_token") != null) {
            tokens.put("access_token", (String) response.get("access_token"));
        }
        if (response.get("refresh_token") != null) {
            tokens.put("refresh_token", (String) response.get("refresh_token"));
        }
        return tokens;
    }

    private void sendEmailKey(String email, String message) {
        int key = random.nextInt(100_000, 999_999);
        EmailKey emailKey = EmailKey.builder()
                .email(email)
                .key(key)
                .storageTime(emailKeysStorageTime)
                .build();
        emailKeysRepository.save(emailKey);
        kafkaTemplate.send("message", new EmailMessageDTO(email, message + key));
    }

}
