package danix.app.authentication_service.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = StartsWithCapitalLaterValidator.class)
public @interface StartsWithCapitalLater {
    String message() default "{javax.validation.constraints.StartsWithCapitalLater.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
