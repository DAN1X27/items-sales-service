package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chats_messages")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String text;

    @Column(name = "sender_id")
    private long senderId;

    @ManyToOne
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private Chat chat;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "content_type")
    @Enumerated(value = EnumType.STRING)
    private ContentType contentType;
}
