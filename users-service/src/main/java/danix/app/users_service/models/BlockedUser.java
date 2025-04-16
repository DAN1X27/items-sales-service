package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blocked_users")
@Getter
@Setter
@NoArgsConstructor
public class BlockedUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "owner_id", referencedColumnName = "id")
	private User owner;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

	public BlockedUser(User ownerId, User userId) {
		this.owner = ownerId;
		this.user = userId;
	}

	public Long getUserId() {
		return user.getId();
	}

}
