package danix.app.email_sender_service.services;

import danix.app.email_sender_service.models.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagesListener {

	private final EmailSenderService emailSenderService;

	@KafkaListener(topics = "message", containerFactory = "messageFactory")
	public void message(EmailMessage emailMessage) {
		emailSenderService.sendMessage(emailMessage.getEmail(), emailMessage.getMessage());
	}

}