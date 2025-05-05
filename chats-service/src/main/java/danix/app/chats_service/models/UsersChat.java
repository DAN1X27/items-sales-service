package danix.app.chats_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "chats")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersChat implements Chat{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "user1_id")
	private long user1Id;

	@Column(name = "user2_id")
	private long user2Id;

	@OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
	private List<ChatMessage> chatMessages;

}
