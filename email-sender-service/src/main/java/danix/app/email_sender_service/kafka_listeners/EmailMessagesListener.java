package danix.app.email_sender_service.kafka_listeners;

import danix.app.email_sender_service.services.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailMessagesListener {

	private final EmailSenderService emailSenderService;

	@KafkaListener(topics = "message", containerFactory = "messageFactory")
	public void receiveMessage(Map<String, String> message) {
		emailSenderService.sendMessage(message.get("email"), message.get("message"));
	}

}