package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String cause;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

	@ManyToOne
	@JoinColumn(name = "sender_id", referencedColumnName = "id")
	private User sender;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

}
