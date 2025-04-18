package danix.app.email_sender_service.services;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderServiceImpl implements EmailSenderService {

	private final JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	private String senderEmail;

	@Override
	public void sendMessage(String to, String message) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			mimeMessage.setFrom(senderEmail);
			mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			mimeMessage.setSubject("Items sales service");
			mimeMessage.setContent(getHtmlContent(message), "text/html;charset=UTF-8");
			javaMailSender.send(mimeMessage);
		}
		catch (Exception e) {
			log.error("Error send message to email - {} : {}", to, e.getMessage(), e);
		}
	}

}
