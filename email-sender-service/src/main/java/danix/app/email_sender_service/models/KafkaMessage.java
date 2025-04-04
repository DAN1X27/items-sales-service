package danix.app.email_sender_service.models;

public class KafkaMessage {
    private String email;
    private String message;

    public KafkaMessage(String email, String message) {
        this.email = email;
        this.message = message;
    }

    public KafkaMessage() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
