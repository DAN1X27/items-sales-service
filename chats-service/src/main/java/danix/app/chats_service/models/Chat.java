package danix.app.chats_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class Chat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "user1_id")
	private long user1Id;

	@Column(name = "user2_id")
	private long user2Id;

	@OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
	private List<ChatMessage> chatMessages;

	public Chat(long user1Id, long user2Id) {
		this.user1Id = user1Id;
		this.user2Id = user2Id;
	}

}
