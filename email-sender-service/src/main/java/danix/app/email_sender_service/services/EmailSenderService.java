package danix.app.email_sender_service.services;

public interface EmailSenderService {

    void sendMessage(String to, String message);

}
