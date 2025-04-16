package danix.app.files_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class FilesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilesServiceApplication.class, args);
	}

}
