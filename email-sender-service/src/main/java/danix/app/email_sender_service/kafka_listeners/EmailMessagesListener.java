package danix.app.email_sender_service.kafka_listeners;

import danix.app.email_sender_service.dto.EmailMessageDTO;
import danix.app.email_sender_service.services.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMessagesListener {

	private final EmailSenderService emailSenderService;

	@KafkaListener(topics = "${kafka-topic}", containerFactory = "messageFactory")
	public void receiveMessage(EmailMessageDTO emailMessage) {
		emailSenderService.sendMessage(emailMessage.getEmail(), emailMessage.getMessage());
	}

}