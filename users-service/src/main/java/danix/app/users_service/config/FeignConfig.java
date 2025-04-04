package danix.app.users_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import danix.app.users_service.util.UserException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.Map;

@Configuration
public class FeignConfig {

    @Bean
    ErrorDecoder errorDecoder() {
        return ((methodKey, response) -> {
            ErrorDecoder decoder = new ErrorDecoder.Default();
            if (response.status() == 400) {
                try (InputStream stream = response.body().asInputStream()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> body = mapper.readValue(stream, new TypeReference<>() {});
                    String message = (String) body.get("message");
                    return new UserException(message);
                } catch (Exception e) {
                    return decoder.decode(methodKey, response);
                }
            }
            return decoder.decode(methodKey, response);
        });
    }
}
