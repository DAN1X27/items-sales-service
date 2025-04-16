package danix.app.announcements_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "announcement_id", referencedColumnName = "id")
	private Announcement announcement;

	private String cause;

	@Column(name = "sender_id")
	private Long senderId;

}
