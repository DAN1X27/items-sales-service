package danix.app.chats_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import danix.app.chats_service.util.ChatException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.Map;

@Configuration
@Slf4j
public class FeignConfig {

	@Bean
	public ErrorDecoder errorDecoder() {
		return ((methodKey, response) -> {
			ErrorDecoder decoder = new ErrorDecoder.Default();
			if (response.status() == 400) {
				try (InputStream stream = response.body().asInputStream()) {
					ObjectMapper mapper = new ObjectMapper();
					Map<String, Object> body = mapper.readValue(stream, new TypeReference<>() {});
					String message = (String) body.get("message");
					return new ChatException(message);
				}
				catch (Exception e) {
					log.error("Error decoding feign error message: {}", e.getMessage(), e);
				}
			}
			return decoder.decode(methodKey, response);
		});
	}

}
