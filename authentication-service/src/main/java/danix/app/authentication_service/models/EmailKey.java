package danix.app.authentication_service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@RedisHash(value = "email_keys")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailKey implements Serializable {

    @Id
    private String email;

    private int key;

    private int attempts;

    @TimeToLive(unit = TimeUnit.MINUTES)
    private int storageTime;
}
