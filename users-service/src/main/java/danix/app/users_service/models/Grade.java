package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grades")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Grade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int stars;

	@ManyToOne
	@JoinColumn(name = "owner_id", referencedColumnName = "id")
	private User owner;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

}
