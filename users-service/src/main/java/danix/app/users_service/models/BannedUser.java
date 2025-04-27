package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banned_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannedUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String cause;

	@OneToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

}
