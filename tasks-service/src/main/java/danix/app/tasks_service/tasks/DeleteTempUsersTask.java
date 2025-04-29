package danix.app.tasks_service.tasks;

import danix.app.tasks_service.feign.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteTempUsersTask {

    private final UsersService usersService;

    @Value("${access_key}")
    private String accessKey;

    @Scheduled(cron = "@midnight")
    public void run() {
        log.info("Start deleting temp users...");
        try {
            usersService.deleteTempUsers(accessKey);
            log.info("Temp users successfully deleted.");
        }
        catch (Exception e) {
            log.error("Error deleting temp users: {}", e.getMessage(), e);
        }
    }

}
