package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;

import java.time.LocalDateTime;

public interface Message {

	long getId();

	String getText();

	ContentType getContentType();

	long getSenderId();

	LocalDateTime getSentTime();

	void setText(String text);

}