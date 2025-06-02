package danix.app.authentication_service.services;

import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.models.EmailKey;

import java.util.Optional;

public interface AuthenticationService {

    JWTTokenDTO login(LoginDTO loginDTO);

    void sendRegistrationKey(RegistrationDTO registrationDTO);

    JWTTokenDTO confirmRegistration(RegistrationEmailKeyDTO emailKeyDTO);

    void sendResetPasswordKey(String email);

    void resetPassword(ResetPasswordDTO resetPasswordDTO);

    void sendUpdateEmailKey(UpdateEmailDTO updateEmailDTO);

    JWTTokenDTO updateEmail(UpdateEmailKeyDTO emailKeyDTO, String token);

    String validateTokenEndGetEmail(String jwtToken);

    void deleteUserTokens();

    void deleteExpiredTokens();

    Optional<EmailKey> getKey(String email);

    void incrementEmailKeyAttempts(EmailKey emailKey);

    void deleteEmailKey(EmailKey emailKey);

}
