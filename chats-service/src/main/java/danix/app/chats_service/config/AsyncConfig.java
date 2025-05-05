package danix.app.chats_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean("virtualExecutor")
    public Executor virualExecutor() {
        return new VirtualThreadTaskExecutor("test - ");
    }

}
