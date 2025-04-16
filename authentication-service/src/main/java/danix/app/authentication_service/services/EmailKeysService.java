package danix.app.authentication_service.services;

import danix.app.authentication_service.dto.EmailMessageDTO;
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
public class EmailKeysService {

	private final KafkaTemplate<String, EmailMessageDTO> kafkaTemplate;

	private final RedisTemplate<String, Object> redisTemplate;

	@Value("${email-keys-storage-time-minutes}")
	private int storageTime;

	public Optional<Integer> getByEmail(String email) {
		Integer key = (Integer) redisTemplate.opsForValue().get(email);
		return key != null ? Optional.of(key) : Optional.empty();
	}

	public void sendKey(String email, String message) {
		Random random = new Random();
		int key = random.nextInt(100_000, 999_999);
		redisTemplate.opsForValue().set(email, key);
		redisTemplate.expire(email, storageTime, TimeUnit.MINUTES);
		kafkaTemplate.send("message", new EmailMessageDTO(email, message + key));
	}

	public void delete(String email) {
		redisTemplate.delete(email);
	}

}