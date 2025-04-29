package danix.app.tasks_service.tasks;

import danix.app.tasks_service.feign.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExpiredAuthenticationTokensTask {

    private final AuthenticationService authenticationService;

    @Value("${access_key}")
    private String accessKey;

    @Scheduled(cron = "@midnight")
    public void run() {
        log.info("Start deleting expired authentication tokens...");
        try {
            authenticationService.deleteExpiredTokens(accessKey);
            log.info("Expired authentication tokens successfully deleted.");
        }
        catch (Exception e) {
            log.error("Error deleting expired authentication tokens: {}", e.getMessage(), e);
        }
    }

}
