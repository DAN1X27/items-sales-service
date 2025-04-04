package danix.app.authentication_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    private final String HOST;
    private final int PORT;
    private final String PASSWORD;
    private final String USERNAME;
    private final int DATA_BASE;

    public RedisConfig(@Value("${redis.host}") String HOST,
                       @Value("${redis.port}") int PORT,
                       @Value("${redis.password}") String PASSWORD,
                       @Value("${redis.username}") String USERNAME,
                       @Value("${redis.database}") int DATA_BASE) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.PASSWORD = PASSWORD;
        this.USERNAME = USERNAME;
        this.DATA_BASE = DATA_BASE;
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setPort(PORT);
        configuration.setHostName(HOST);
        configuration.setDatabase(DATA_BASE);
        configuration.setUsername(USERNAME);
        configuration.setPassword(PASSWORD);
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }
}