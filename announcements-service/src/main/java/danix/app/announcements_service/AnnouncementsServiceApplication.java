package danix.app.announcements_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableKafka
public class AnnouncementsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnnouncementsServiceApplication.class, args);
	}

}