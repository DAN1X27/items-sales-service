package danix.app.users_service.task;

import danix.app.users_service.models.User;
import danix.app.users_service.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeleteTemporalRegisteredUsersTask {

    private final UsersRepository usersRepository;

    @Transactional
    @Scheduled(cron = "@midnight")
    public void run() {
        usersRepository.deleteAllByStatusAndRegisteredAtBefore(User.Status.TEMPORALLY_REGISTERED,
                LocalDateTime.now().minusDays(1));
    }
}
