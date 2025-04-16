package danix.app.announcements_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "announcements_images")
@NoArgsConstructor
@Getter
@Setter
public class Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name")
	private String fileName;

	@ManyToOne
	@JoinColumn(name = "announcement_id", referencedColumnName = "id")
	private Announcement announcement;

	public Image(String fileName, Announcement announcement) {
		this.fileName = fileName;
		this.announcement = announcement;
	}

}
