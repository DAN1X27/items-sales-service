package danix.app.announcements_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcements_images")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "announcement_id", referencedColumnName = "id")
    private Announcement announcement;
}
