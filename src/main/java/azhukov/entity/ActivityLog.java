package azhukov.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** JPA сущность для ленты активности пользователей */
@Entity
@Table(name = "activity_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"metadata"})
public class ActivityLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "activity_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ActivityType activityType;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "entity_type", length = 50)
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Column(name = "metadata", columnDefinition = "JSONB")
  private String metadata; // JSON строка

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Типы активности */
  public enum ActivityType {
    INTERVIEW,
    POSITION,
    CANDIDATE,
    HIRED,
    REPORT,
    LOGIN
  }

  /** Типы сущностей */
  public enum EntityType {
    INTERVIEW,
    POSITION,
    CANDIDATE,
    REPORT
  }
}
