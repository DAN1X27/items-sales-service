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
public class EmailSenderService {

	private final JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	private String senderEmail;

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
			log.error("Error send message to email - {}, error - {}", to, e.getMessage(), e);
		}
	}

	private String getHtmlContent(String message) {
		return String.format(
			"""
            <html>
                <head>
                    <style>
                        body {
                            color: white;
                            background-color: #1a1a1a;
                            font-family: Arial, sans-serif;
                        }
                        .container {
                            background-color: #000720;
                            color: white;
                            padding: 20px;
                            border-radius: 10px;
                            border: 2px solid #ffcc00;
                            display: inline-block;
                            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
                            margin-top: 20px;
                        }
                        h1 {
                            margin: 0;
                            text-align: center;
                            font-weight: bold;
                        }
                        .message {
                            font-size: 25px;
                            font-weight: normal;
                            white-space: pre-wrap;
                        }
                    </style>
                </head>
                <body style = "text-align: center">
                    <div class="container">
                        <h1>Items sales service</h1>
                        <p class="message">%s</p>
                    </div>
                </body>
            </html>
            """, message
		);
	}

}
