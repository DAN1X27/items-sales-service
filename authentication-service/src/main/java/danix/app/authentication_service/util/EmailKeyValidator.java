package danix.app.authentication_service.util;

import danix.app.authentication_service.dto.EmailKey;
import danix.app.authentication_service.services.EmailKeysService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class EmailKeyValidator implements Validator {

	private final EmailKeysService emailKeysService;

	@Override
	public boolean supports(Class<?> clazz) {
		return EmailKey.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		EmailKey keyDTO = (EmailKey) target;
		emailKeysService.getByEmail(keyDTO.getEmail()).ifPresentOrElse(key -> {
			if (!key.equals(keyDTO.getKey())) {
				errors.rejectValue("key", "", "Incorrect key");
			}
		}, () -> errors.rejectValue("email", "", "Email not found"));
	}

}
