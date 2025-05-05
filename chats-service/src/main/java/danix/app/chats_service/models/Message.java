package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;

import java.time.LocalDateTime;

public interface Message {

	long getId();

	String getText();

	ContentType getContentType();

	long getSenderId();

	LocalDateTime getSentTime();

	void setId(long id);

	void setText(String text);

	void setContentType(ContentType contentType);

	void setSenderId(long id);

	void setSentTime(LocalDateTime sentTime);
}