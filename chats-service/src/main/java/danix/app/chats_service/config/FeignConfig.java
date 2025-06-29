package danix.app.chats_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ErrorCode;
import danix.app.chats_service.util.ErrorData;
import danix.app.chats_service.util.RequestException;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class FeignConfig {

	private final ObjectMapper mapper;

	@Bean
	public ErrorDecoder errorDecoder() {
		return ((methodKey, response) -> {
			ErrorDecoder decoder = new ErrorDecoder.Default();
			if (response.status() == HttpServletResponse.SC_BAD_REQUEST) {
				try (InputStream stream = response.body().asInputStream()) {
					JsonNode body = mapper.readTree(stream);
					int code = Integer.parseInt(body.get("code").toString());
					if (code == ErrorCode.REQUEST_ERROR.getCode()) {
						Map<String, Map<String, String>> error = mapper.readValue(body.get("error").toString(),
                                new TypeReference<>() {});
						Map<String, ErrorData> decodedError = new HashMap<>();
						error.forEach((field, errorInfo) -> {
							String message = errorInfo.get("message");
							LocalDateTime timestamp = LocalDateTime.parse(errorInfo.get("timestamp"));
							decodedError.put(field, new ErrorData(message, timestamp));
						});
						return new RequestException(decodedError);
					}
					Map<String, String> error = mapper.readValue(body.get("error").toString(), new TypeReference<>() {});
					return new ChatException(error.get("message"));
				}
				catch (Exception e) {
					log.error("Error decoding feign error message: {}", e.getMessage(), e);
				}
			}
			return decoder.decode(methodKey, response);
		});
	}

}
