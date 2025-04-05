package danix.app.authentication_service.util;

import danix.app.authentication_service.dto.EmailKeyDTO;
import danix.app.authentication_service.dto.RegistrationKeyDTO;
import danix.app.authentication_service.services.EmailKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class EmailKeyValidator implements Validator {

    private final EmailKeyService emailKeyService;

    @Override
    public boolean supports(Class<?> clazz) {
        return EmailKeyDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EmailKeyDTO keyDTO = (EmailKeyDTO) target;
        emailKeyService.getByEmail(keyDTO.email()).ifPresentOrElse(key -> {
            if (!key.equals(keyDTO.key())) {
                errors.rejectValue("key", "", "Incorrect key");
            }
        }, () -> errors.rejectValue("email", "", "Email not found"));
    }
}
