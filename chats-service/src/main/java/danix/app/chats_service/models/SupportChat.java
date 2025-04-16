package danix.app.chats_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "support_chats")
@Getter
@Setter
@NoArgsConstructor
public class SupportChat {

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

	public SupportChat(long userId, Status status) {
		this.userId = userId;
		this.status = status;
	}

	public enum Status {

		WAIT,

		IN_PROCESSING,

		CLOSED

	}

}
