package danix.app.authentication_service.services;

import danix.app.authentication_service.dto.*;
import danix.app.authentication_service.models.EmailKey;

import java.util.Optional;

public interface AuthenticationService {

    TokensDTO login(LoginDTO loginDTO);

    void logout(String refreshToken);

    void disableUser(String email);

    void enableUser(String email);

    void tempRegistration(RegistrationDTO registrationDTO);

    void confirmRegistration(RegistrationEmailKeyDTO emailKeyDTO);

    TokensDTO refreshToken(String refreshToken);

    void sendResetPasswordKey(String email);

    void resetPassword(ResetPasswordDTO resetPasswordDTO);

    void updatePassword(UpdatePasswordDTO updatePasswordDTO);

    void sendUpdateEmailKey(UpdateEmailDTO updateEmailDTO);

    void updateEmail(UpdateEmailKeyDTO emailKeyDTO);

    void updateUserInfo(UpdateUserInfoDTO updateUserInfoDTO);

    Optional<EmailKey> getKey(String email);

    void incrementEmailKeyAttempts(EmailKey emailKey);

    void deleteEmailKey(EmailKey emailKey);

    void deleteUser(String email);

}
