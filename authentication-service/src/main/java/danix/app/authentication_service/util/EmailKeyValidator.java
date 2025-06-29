package danix.app.authentication_service.util;

import danix.app.authentication_service.dto.EmailKeyDTO;
import danix.app.authentication_service.dto.RegistrationEmailKeyDTO;
import danix.app.authentication_service.feign.UsersAPI;
import danix.app.authentication_service.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class EmailKeyValidator implements Validator {

	private final AuthenticationService authenticationService;

	private final UsersAPI usersAPI;

	@Value("${access_key}")
	private String accessKey;

	@Value("${max-email-key-attempts}")
	private int maxAttempts;

	@Override
	public boolean supports(Class<?> clazz) {
		return EmailKeyDTO.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		EmailKeyDTO keyDTO = (EmailKeyDTO) target;
		authenticationService.getKey(keyDTO.getEmail()).ifPresentOrElse(emailKey -> {
			if (emailKey.getKey() != keyDTO.getKey()) {
				authenticationService.incrementEmailKeyAttempts(emailKey);
				if (emailKey.getAttempts() >= maxAttempts) {
					authenticationService.deleteEmailKey(emailKey);
					if (keyDTO instanceof RegistrationEmailKeyDTO) {
						usersAPI.deleteTempUser(emailKey.getEmail(), accessKey);
						authenticationService.deleteUser(keyDTO.getEmail());
					}
					errors.rejectValue("key", "", "Attempt limit reached");
				} else {
					errors.rejectValue("key", "", "Incorrect key");
				}
			}
		}, () -> errors.rejectValue("email", "", "Email not found"));
	}

}
