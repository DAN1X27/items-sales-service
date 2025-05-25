package danix.app.email_sender_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessagesListener {

	private final EmailSenderService emailSenderService;

	@KafkaListener(topics = "message", containerFactory = "messageFactory")
	public void message(Map<String, String> message) {
		emailSenderService.sendMessage(message.get("email"), message.get("message"));
	}

}