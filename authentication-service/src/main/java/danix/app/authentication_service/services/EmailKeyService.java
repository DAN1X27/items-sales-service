package danix.app.authentication_service.services;

import danix.app.authentication_service.util.KafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailKeyService {
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${email-keys-storage-time-minutes}")
    private int storageTime;

    public Optional<Integer> getByEmail(String email) {
        Integer key = (Integer) redisTemplate.opsForValue().get(email);
        return key != null ? Optional.of(key) : Optional.empty();
    }

    public void save(String email) {
        Random random = new Random();
        Integer key = random.nextInt(100000, 999999);
        redisTemplate.opsForValue().set(email, key);
        redisTemplate.expire(email, storageTime, TimeUnit.MINUTES);
        String message = "Your registration key, dont show it to anyone: " + key;
        kafkaTemplate.send("message", new KafkaMessage(email, message));
    }

    public void delete(String email) {
        redisTemplate.delete(email);
    }
}