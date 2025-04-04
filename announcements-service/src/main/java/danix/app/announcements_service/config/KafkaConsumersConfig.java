package danix.app.announcements_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumersConfig {

    private final String BOOTSTRAP_ADDRESS;
    private final String GROUP_ID;

    public KafkaConsumersConfig(@Value("${spring.kafka.consumer.bootstrap-servers}") String BOOTSTRAP_ADDRESS,
                                @Value("${spring.kafka.consumer.group-id}") String GROUP_ID) {
        this.BOOTSTRAP_ADDRESS = BOOTSTRAP_ADDRESS;
        this.GROUP_ID = GROUP_ID;
    }

    @Bean
    public ConsumerFactory<String, Long> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_ADDRESS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new LongDeserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Long> userFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Long> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
