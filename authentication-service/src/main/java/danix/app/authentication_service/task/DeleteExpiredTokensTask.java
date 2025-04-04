package danix.app.authentication_service.task;

import danix.app.authentication_service.services.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteExpiredTokensTask {
    private final TokenService tokenService;

    @Scheduled(cron = "@midnight")
    public void run() {
        tokenService.deleteExpiredTokens();
    }
}
