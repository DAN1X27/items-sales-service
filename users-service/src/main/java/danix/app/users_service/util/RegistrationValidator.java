package danix.app.users_service.util;

import danix.app.users_service.dto.RegistrationDTO;
import danix.app.users_service.models.User;
import danix.app.users_service.repositories.UsersRepository;
import danix.app.users_service.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class RegistrationValidator implements Validator {

	private final UsersRepository usersRepository;

	private final UsersService usersService;

	@Override
	public boolean supports(Class<?> clazz) {
		return RegistrationDTO.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		RegistrationDTO userDTO = (RegistrationDTO) target;
		usersRepository.findByEmail(userDTO.getEmail()).ifPresent(user -> {
			if (user.getStatus() == User.Status.REGISTERED) {
				errors.rejectValue("email", "", "This email is already in use");
			}
			else {
				usersService.deleteTempUser(user);
			}
		});
		usersRepository.findByUsername(userDTO.getUsername()).ifPresent(user -> {
			if (user.getStatus() == User.Status.REGISTERED) {
				errors.rejectValue("username", "", "This username is already in use");
			}
			else {
				usersService.deleteTempUser(user);
			}
		});
	}

}
