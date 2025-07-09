package danix.app.authentication_service;

import danix.app.authentication_service.config.KeycloakProperties;
import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.feign.KeycloakAPI;
import danix.app.authentication_service.feign.UsersAPI;
import danix.app.authentication_service.keycloak_dto.KeycloakRegistrationDTO;
import danix.app.authentication_service.keycloak_dto.UserAttributesDTO;
import danix.app.authentication_service.keycloak_dto.UserInfoDTO;
import danix.app.authentication_service.mapper.RegistrationMapper;
import danix.app.authentication_service.models.EmailKey;
import danix.app.authentication_service.repositories.EmailKeysRepository;
import danix.app.authentication_service.services.impl.AuthenticationServiceImpl;
import danix.app.authentication_service.util.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTests {

    @Mock
    private KafkaTemplate<String, EmailMessageDTO> kafkaTemplate;

    @Mock
    private UsersAPI usersAPI;

    @Mock
    private EmailKeysRepository emailKeysRepository;

    @Mock
    private KeycloakProperties keycloakProperties;

    @Mock
    private KeycloakAPI keycloakAPI;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private final String accessKey = "access_key";

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(authenticationService, "accessKey", accessKey);
        ReflectionTestUtils.setField(authenticationService, "emailKeysStorageTime", 3);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void login() {
        LoginDTO loginDTO = LoginDTO.builder()
                .username("user")
                .password("password")
                .build();
        when(usersAPI.isUserBanned(loginDTO.getUsername(), accessKey)).thenReturn(Map.of("is_banned", false));
        Map<String, Object> testResponse = Map.of(
                "access_token", "access_token",
                "refresh_token", "refresh_token"
        );
        when(keycloakAPI.getTokens(any())).thenReturn(testResponse);
        assertDoesNotThrow(() -> authenticationService.login(loginDTO));
    }

    @Test
    public void loginWhenUserIsBanned() {
        LoginDTO loginDTO = LoginDTO.builder()
                .username("user")
                .password("password")
                .build();
        Map<String, Object> testResponse = Map.of(
                "is_banned", true,
                "cause", "test"
        );
        when(usersAPI.isUserBanned(loginDTO.getUsername(), accessKey)).thenReturn(testResponse);
        assertThrows(AuthenticationException.class, () -> authenticationService.login(loginDTO));
    }

    @Test
    public void tempRegistration() {
        RegistrationDTO registrationDTO = new RegistrationDTO();
        registrationDTO.setEmail("email");
        TempRegistrationDTO tempRegistrationDTO = new TempRegistrationDTO();
        UserAttributesDTO attributes = new UserAttributesDTO();
        KeycloakRegistrationDTO keycloakRegistrationDTO = new KeycloakRegistrationDTO();
        when(emailKeysRepository.findById(registrationDTO.getEmail())).thenReturn(Optional.empty());
        when(registrationMapper.toTempRegistrationDTO(registrationDTO)).thenReturn(tempRegistrationDTO);
        when(registrationMapper.toAttributes(registrationDTO)).thenReturn(attributes);
        when(registrationMapper.toRegistration(eq(registrationDTO), eq(attributes), any()))
                .thenReturn(keycloakRegistrationDTO);
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        authenticationService.tempRegistration(registrationDTO);
        verify(usersAPI).tempRegistration(tempRegistrationDTO, accessKey);
        verify(keycloakAPI).registration(eq(keycloakRegistrationDTO), any());
        verify(emailKeysRepository).save(any());
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    public void tempRegistrationWhenUserHasKey() {
        RegistrationDTO registrationDTO = new RegistrationDTO();
        registrationDTO.setEmail("email");
        when(emailKeysRepository.findById(registrationDTO.getEmail())).thenReturn(Optional.of(new EmailKey()));
        assertThrows(AuthenticationException.class, () -> authenticationService.tempRegistration(registrationDTO));
    }

    @Test
    public void confirmRegistration() {
        RegistrationEmailKeyDTO emailKeyDTO = new RegistrationEmailKeyDTO();
        emailKeyDTO.setEmail("email");
        Long id = 1L;
        UserInfoDTO userInfoDTO = getTestUserInfo();
        userInfoDTO.setEnabled(false);
        userInfoDTO.getAttributes().setId(null);
        when(usersAPI.registrationConfirm(emailKeyDTO.getEmail(), accessKey)).thenReturn(Map.of("data", id));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(eq(emailKeyDTO.getEmail()), any())).thenReturn(List.of(userInfoDTO));
        authenticationService.confirmRegistration(emailKeyDTO);
        verify(keycloakAPI).updateUserInfo(eq(userInfoDTO.getId()), eq(userInfoDTO), any());
        verify(emailKeysRepository).deleteById(emailKeyDTO.getEmail());
        assertEquals(List.of(id), userInfoDTO.getAttributes().getId());
        assertTrue(userInfoDTO.isEnabled());
    }

    @Test
    public void confirmRegistrationWhenThrowsException() {
        RegistrationEmailKeyDTO emailKeyDTO = new RegistrationEmailKeyDTO();
        emailKeyDTO.setEmail("email");
        Long id = 1L;
        UserInfoDTO userInfoDTO = getTestUserInfo();
        when(usersAPI.registrationConfirm(emailKeyDTO.getEmail(), accessKey)).thenReturn(Map.of("data", id));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(eq(emailKeyDTO.getEmail()), any())).thenReturn(List.of(userInfoDTO));
        doThrow(new RuntimeException())
                .when(keycloakAPI)
                .updateUserInfo(eq(userInfoDTO.getId()), eq(userInfoDTO), any());
        assertThrows(RuntimeException.class, () -> authenticationService.confirmRegistration(emailKeyDTO));
        verify(usersAPI).deleteUser(id, accessKey);
        verify(keycloakAPI).deleteUser(eq(userInfoDTO.getId()), any());
        verify(emailKeysRepository).deleteById(emailKeyDTO.getEmail());
    }

    @Test
    public void confirmRegistrationWhenUserNotFound() {
        RegistrationEmailKeyDTO emailKeyDTO = new RegistrationEmailKeyDTO();
        emailKeyDTO.setEmail("email");
        when(usersAPI.registrationConfirm(emailKeyDTO.getEmail(), accessKey)).thenReturn(Map.of("data", 1L));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(eq(emailKeyDTO.getEmail()), any())).thenReturn(List.of());
        assertThrows(AuthenticationException.class, () -> authenticationService.confirmRegistration(emailKeyDTO));
    }

    @Test
    public void refreshToken() {
        Map<String, Object> testResponse = Map.of(
                "access_token", "access_token",
                "refresh_token", "refresh_token"
        );
        when(keycloakAPI.getTokens(any())).thenReturn(testResponse);
        assertDoesNotThrow(() -> authenticationService.refreshToken("refresh_token"));
    }

    @Test
    public void sendResetPasswordKey() {
        UserInfoDTO userInfoDTO = getTestUserInfo();
        when(keycloakAPI.getUsersByEmail(eq(userInfoDTO.getEmail()), any())).thenReturn(List.of(userInfoDTO));
        when(emailKeysRepository.findById(userInfoDTO.getEmail())).thenReturn(Optional.empty());
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        authenticationService.sendResetPasswordKey(userInfoDTO.getEmail());
        verify(emailKeysRepository).save(any());
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    public void sendResetPasswordKeyWhenUserNotFound() {
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of());
        assertThrows(AuthenticationException.class, () -> authenticationService.sendResetPasswordKey("email"));
    }

    @Test
    public void sendResetPasswordKeyWhenUserHasKey() {
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of(getTestUserInfo()));
        when(emailKeysRepository.findById(any())).thenReturn(Optional.of(new EmailKey()));
        assertThrows(AuthenticationException.class, () -> authenticationService.sendResetPasswordKey("email"));
    }

    @Test
    public void resetPassword() {
        mockJwt(Map.of(JwtClaimNames.SUB, "id"));
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        authenticationService.resetPassword(resetPasswordDTO);
        verify(emailKeysRepository).deleteById(resetPasswordDTO.getEmail());
        verify(keycloakAPI).resetPassword(any(), any(), any());
    }

    @Test
    public void updatePassword() {
        mockJwt(Map.of("email", "email", JwtClaimNames.SUB, "id"));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        UpdatePasswordDTO updatePasswordDTO = new UpdatePasswordDTO();
        updatePasswordDTO.setOldPassword("old_password");
        updatePasswordDTO.setNewPassword("new_password");
        authenticationService.updatePassword(updatePasswordDTO);
        verify(keycloakAPI).resetPassword(any(), any(), any());
    }

    @Test
    public void sendUpdateEmailKey() {
        UpdateEmailDTO updateEmailDTO = new UpdateEmailDTO();
        updateEmailDTO.setEmail("email");
        when(emailKeysRepository.findById(updateEmailDTO.getEmail())).thenReturn(Optional.empty());
        authenticationService.sendUpdateEmailKey(updateEmailDTO);
        verify(emailKeysRepository).save(any());
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    public void sendUpdateEmailKeyWhenUserHasKey() {
        UpdateEmailDTO updateEmailDTO = new UpdateEmailDTO();
        updateEmailDTO.setEmail("new_email");
        when(emailKeysRepository.findById(updateEmailDTO.getEmail())).thenReturn(Optional.of(new EmailKey()));
        assertThrows(AuthenticationException.class, () -> authenticationService.sendUpdateEmailKey(updateEmailDTO));
    }

    @Test
    public void updateEmail() {
        UpdateEmailKeyDTO updateEmailKeyDTO = new UpdateEmailKeyDTO();
        updateEmailKeyDTO.setEmail("new_email");
        mockJwtWithTokenValue(Map.of("email", "email", JwtClaimNames.SUB, "id"));
        UserInfoDTO userInfoDTO = getTestUserInfo();
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of(userInfoDTO));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        authenticationService.updateEmail(updateEmailKeyDTO);
        verify(emailKeysRepository).deleteById(updateEmailKeyDTO.getEmail());
        verify(usersAPI).updateEmail(eq(updateEmailKeyDTO.getEmail()), eq(accessKey), any());
        verify(keycloakAPI).updateUserInfo(any(), any(), any());
        assertEquals(updateEmailKeyDTO.getEmail(), userInfoDTO.getEmail());
    }

    @Test
    public void updateEmailWhenUserNotFound() {
        UpdateEmailKeyDTO updateEmailKeyDTO = new UpdateEmailKeyDTO();
        updateEmailKeyDTO.setEmail("new_email");
        mockJwtWithTokenValue(Map.of("email", "email"));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of());
        assertThrows(AuthenticationException.class, () -> authenticationService.updateEmail(updateEmailKeyDTO));
    }

    @Test
    public void updateEmailWhenThrowsException() {
        UpdateEmailKeyDTO updateEmailKeyDTO = new UpdateEmailKeyDTO();
        updateEmailKeyDTO.setEmail("new_email");
        mockJwtWithTokenValue(Map.of("email", "email"));
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of());
        assertThrows(AuthenticationException.class, () -> authenticationService.updateEmail(updateEmailKeyDTO));
        verify(usersAPI).updateEmail(eq(updateEmailKeyDTO.getEmail()), eq(accessKey), any());
        verify(usersAPI).updateEmail(eq("email"), eq(accessKey), any());
    }

    @Test
    public void updateUserInfo() {
        UpdateUserInfoDTO updateUserInfoDTO = UpdateUserInfoDTO.builder()
                .username("new_username")
                .city("new_city")
                .country("new_country")
                .build();
        mockJwt(Map.of("email", "email"));
        UserInfoDTO userInfoDTO = getTestUserInfo();
        when(keycloakAPI.getTokens(any())).thenReturn(Map.of("access_token", "access_token"));
        when(keycloakAPI.getUsersByEmail(any(), any())).thenReturn(List.of(userInfoDTO));
        authenticationService.updateUserInfo(updateUserInfoDTO);
        verify(keycloakAPI).updateUserInfo(any(), any(), any());
        UserAttributesDTO attributesDTO = userInfoDTO.getAttributes();
        assertEquals(updateUserInfoDTO.getUsername(), userInfoDTO.getUsername());
        assertEquals(List.of(updateUserInfoDTO.getCountry()), attributesDTO.getCountry());
        assertEquals(List.of(updateUserInfoDTO.getCity()), attributesDTO.getCity());
        assertNotNull(attributesDTO.getFirstName());
        assertNotNull(attributesDTO.getLastName());
    }

    private void mockJwtWithTokenValue(Map<String, Object> claims) {
        mockJwt(claims);
        when(jwt.getTokenValue()).thenReturn("jwt");
    }

    private void mockJwt(Map<String, Object> claims) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        claims.forEach((name, value) -> when(jwt.getClaim(name)).thenReturn(value));
    }

    private UserInfoDTO getTestUserInfo() {
        UserAttributesDTO attributes = UserAttributesDTO.builder()
                .id(List.of(1L))
                .firstName(List.of("First name"))
                .lastName(List.of("Last name"))
                .country(List.of("country"))
                .city(List.of("city"))
                .build();
        return UserInfoDTO.builder()
                .username("user")
                .email("email")
                .enabled(true)
                .id("id")
                .attributes(attributes)
                .build();
    }
}
