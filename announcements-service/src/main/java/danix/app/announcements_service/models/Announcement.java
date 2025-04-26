package danix.app.announcements_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "announcements")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Announcement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	private String description;

	private Double price;

	@Column(name = "owner_id")
	private Long ownerId;

	private String type;

	@Column(name = "phone_number")
	private String phoneNumber;

	private String country;

	private String city;

	@Column(name = "likes")
	private int likesCount;

	@Column(name = "watches")
	private int watchesCount;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL)
	private List<Image> images;

	@OneToMany(mappedBy = "announcement")
	private List<Like> likes;

	@OneToMany(mappedBy = "announcement")
	private List<Watch> watches;

}
