package danix.app.announcements_service.config;

import danix.app.announcements_service.dto.EmailMessageDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaProducersConfig {

	@Value("${spring.kafka.consumer.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public ProducerFactory<String, List<String>> listProducerFactory() {
		return new DefaultKafkaProducerFactory<>(getProps());
	}

	@Bean
	public ProducerFactory<String, EmailMessageDTO> emailMessageFactory() {
		return new DefaultKafkaProducerFactory<>(getProps());
	}

	@Bean
	public KafkaTemplate<String, List<String>> listKafkaTemplate() {
		return new KafkaTemplate<>(listProducerFactory());
	}

	@Bean
	public KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate() {
		return new KafkaTemplate<>(emailMessageFactory());
	}

	Map<String, Object> getProps() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		return props;
	}
}
