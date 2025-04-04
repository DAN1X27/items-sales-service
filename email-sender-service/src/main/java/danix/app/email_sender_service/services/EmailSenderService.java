package danix.app.email_sender_service.services;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final JavaMailSender javaMailSender;
    private final String SENDER_EMAIL;
    private static final Logger LOG = LoggerFactory.getLogger(EmailSenderService.class);

    public EmailSenderService(JavaMailSender javaMailSender, @Value("${spring.mail.username}") String SENDER_EMAIL) {
        this.javaMailSender = javaMailSender;
        this.SENDER_EMAIL = SENDER_EMAIL;
    }

    public void sendMessage(String to, String message) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            mimeMessage.setFrom(SENDER_EMAIL);
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setSubject("Spring messenger service application");
            mimeMessage.setContent(
                    """
                        <body style = "color: white; text-align: center">
                                <h1 style = "color: #000720;">Items sales service</h1>
                                <p style="background-color: #000720; font-size: 25px; display: inline;">""" + message + """
                                </p>
                        </body>
                    """,
                    "text/html;charset=UTF-8");
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error(e.getMessage());
        }
    }
}
