package danix.app.chats_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "support_chats")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupportChat implements Chat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "user_id")
	private long userId;

	@Column(name = "admin_id")
	private Long adminId;

	@Enumerated(EnumType.STRING)
	private Status status;

	@OneToMany(mappedBy = "chat")
	private List<SupportChatMessage> messages;

	public enum Status {

		WAIT,

		IN_PROCESSING,

		CLOSED

	}

}
