package danix.app.email_sender_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class EmailSenderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailSenderServiceApplication.class, args);
	}

}
