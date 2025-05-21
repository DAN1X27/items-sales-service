package danix.app.authentication_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeletedEmailKeysListener implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        if (key.startsWith("email_keys")) {
            redisTemplate.opsForSet().remove("email_keys", key);
        }
    }
}
