package danix.app.authentication_service.util;

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
        return RegistrationKeyDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationKeyDTO keyDTO = (RegistrationKeyDTO) target;
        emailKeyService.getByEmail(keyDTO.getEmail()).ifPresentOrElse(key -> {
            if (!key.equals(keyDTO.getKey())) {
                errors.rejectValue("key", "", "Incorrect key");
            }
        }, () -> errors.rejectValue("email", "", "Email not found"));
    }
}
