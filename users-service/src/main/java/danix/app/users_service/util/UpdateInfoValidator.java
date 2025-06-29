package danix.app.users_service.util;

import danix.app.users_service.dto.UpdateInfoDTO;
import danix.app.users_service.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class UpdateInfoValidator implements Validator {

    private final UsersRepository usersRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return UpdateInfoDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UpdateInfoDTO updateInfoDTO = (UpdateInfoDTO) target;
        usersRepository.findByUsername(updateInfoDTO.getUsername()).ifPresent(user -> errors
                .rejectValue("username", "", "This username is already in use"));
    }
}
