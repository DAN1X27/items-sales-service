package danix.app.users_service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@RedisHash("temp_users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempUser implements Serializable {

    @Id
    private String email;

    private String username;

    private String password;

    private String country;

    private String city;

    private User.Role role;

    private LocalDateTime registeredAt;

    @TimeToLive(unit = TimeUnit.MINUTES)
    private int liveTime;
}
