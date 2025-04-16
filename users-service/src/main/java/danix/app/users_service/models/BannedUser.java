package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "banned_users")
@NoArgsConstructor
@Getter
@Setter
public class BannedUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String cause;

	@OneToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

	public BannedUser(String cause, User user) {
		this.cause = cause;
		this.user = user;
	}

}
