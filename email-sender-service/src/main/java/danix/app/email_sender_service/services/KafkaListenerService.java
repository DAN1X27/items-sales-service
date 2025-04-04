package danix.app.email_sender_service.services;

import danix.app.email_sender_service.models.KafkaMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaListenerService {

    private final EmailSenderService emailSenderService;

    public KafkaListenerService(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @KafkaListener(topics = "message", containerFactory = "messageFactory", groupId = "groupId")
    public void message(KafkaMessage kafkaMessage) {
        emailSenderService.sendMessage(
                kafkaMessage.getEmail(),
                kafkaMessage.getMessage()
        );
    }
}
