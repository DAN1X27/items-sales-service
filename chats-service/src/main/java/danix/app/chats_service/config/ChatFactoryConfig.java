package danix.app.chats_service.config;

import danix.app.chats_service.models.ChatFactory;
import danix.app.chats_service.models.ChatType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ChatFactoryConfig {

    @Bean
    public Map<ChatType, ChatFactory> chatFactoryMap(List<ChatFactory> factories) {
        return factories.stream()
                .collect(Collectors.toMap(ChatFactory::getChatType, Function.identity()));
    }

}
