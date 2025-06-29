package danix.app.users_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String email;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "registered_at")
	private LocalDateTime registeredAt;

	private String avatar;

	private String country;

	private String city;

	private Double grade;

	@OneToMany(mappedBy = "user")
	private List<Comment> comments;

	@OneToMany(mappedBy = "owner")
	private List<Comment> commentsByUser;

	@OneToMany(mappedBy = "user")
	private List<Grade> grades;

	@OneToMany(mappedBy = "owner")
	private List<Grade> gradesByUser;

	@OneToMany(mappedBy = "owner")
	private List<BlockedUser> blockedUsers;

}
