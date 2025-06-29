package danix.app.authentication_service.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StartsWithCapitalLaterValidator implements ConstraintValidator<StartsWithCapitalLater, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        char firstLetter = value.charAt(0);
        return Character.isUpperCase(firstLetter);
    }
}
