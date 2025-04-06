package danix.app.chats_service.models;

import danix.app.chats_service.util.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_chats_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private SupportChat chat;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "sender_id")
    private long senderId;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;
}
