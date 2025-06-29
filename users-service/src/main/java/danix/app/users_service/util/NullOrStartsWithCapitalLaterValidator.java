package danix.app.users_service.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullOrStartsWithCapitalLaterValidator implements ConstraintValidator<NullOrStartsWithCapitalLater, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        char firstLetter = value.charAt(0);
        return Character.isUpperCase(firstLetter);
    }
}
