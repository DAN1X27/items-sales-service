package danix.app.files_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("paths")
@Data
public class AppProperties {

    private String usersAvatars;

    private String chatsImages;

    private String chatsVideos;

    private String announcementsImages;
}
