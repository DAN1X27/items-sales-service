package danix.app.users_service.util;

import danix.app.users_service.dto.RegistrationDTO;
import danix.app.users_service.repositories.TempUsersRepository;
import danix.app.users_service.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class RegistrationValidator implements Validator {

	private final UsersRepository usersRepository;

	private final TempUsersRepository tempUsersRepository;

	@Override
	public boolean supports(Class<?> clazz) {
		return RegistrationDTO.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		RegistrationDTO userDTO = (RegistrationDTO) target;
		if (usersRepository.findByEmail(userDTO.getEmail()).isPresent() ||
			tempUsersRepository.findById(userDTO.getEmail()).isPresent()) {
			errors.rejectValue("email", "", "This email is already in use");
		} else {
			usersRepository.findByUsername(userDTO.getUsername()).ifPresent(user -> errors
					.rejectValue("username", "", "This username is already in use"));
		}
	}

}
