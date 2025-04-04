package danix.app.announcements_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "announcements_watches")
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", referencedColumnName = "id")
    private Announcement announcement;

    @Column(name = "user_id")
    private Long userId;

    public AnnouncementWatch(Announcement announcement, Long userId) {
        this.announcement = announcement;
        this.userId = userId;
    }
}
