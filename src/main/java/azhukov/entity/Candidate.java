package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Сущность кандидата в системе. Представляет человека, проходящего собеседование на вакансию. */
@Entity
@Table(
    name = "candidates",
    indexes = {
      @Index(name = "idx_candidates_position_id", columnList = "position_id"),
      @Index(name = "idx_candidates_status", columnList = "status"),
      @Index(name = "idx_candidates_email", columnList = "email", unique = true),
      @Index(name = "idx_candidates_phone", columnList = "phone", unique = true),
      @Index(name = "idx_candidates_created_at", columnList = "created_at")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"position", "interviews"})
public class Candidate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Имя кандидата обязательно")
  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @NotBlank(message = "Фамилия кандидата обязательна")
  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Email(message = "Некорректный email")
  @Column(length = 255)
  private String email;

  @Column(length = 20)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Статус кандидата обязателен")
  @Builder.Default
  private Status status = Status.NEW;

  @Column(name = "resume_url")
  private String resumeUrl;

  @Column(name = "cover_letter", columnDefinition = "TEXT")
  private String coverLetter;

  @Column(name = "experience_years")
  private Integer experienceYears;

  @Column(name = "source", length = 50)
  private String source;

  @ElementCollection
  @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
  @Column(name = "skill", length = 100)
  @Builder.Default
  private List<String> skills = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "position_id", nullable = false)
  @NotNull(message = "Вакансия обязательна")
  private Position position;

  @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Interview> interviews = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Статусы кандидата */
  public enum Status {
    NEW, // Новый кандидат
    IN_PROGRESS, // В процессе собеседования
    FINISHED, // Собеседование завершено
    REJECTED, // Отклонен
    HIRED // Принят на работу
  }

  /** Полное имя кандидата */
  public String getFullName() {
    return firstName + " " + lastName;
  }

  /** Добавляет навык кандидату */
  public void addSkill(String skill) {
    if (skill != null && !skill.trim().isEmpty()) {
      skills.add(skill.trim());
    }
  }

  /** Удаляет навык у кандидата */
  public void removeSkill(String skill) {
    skills.remove(skill);
  }

  /** Проверяет, есть ли у кандидата активное собеседование */
  public boolean hasActiveInterview() {
    return interviews.stream()
        .anyMatch(interview -> interview.getStatus() == Interview.Status.IN_PROGRESS);
  }

  /** Получает последнее собеседование кандидата */
  public Interview getLatestInterview() {
    return interviews.stream()
        .max((i1, i2) -> i1.getCreatedAt().compareTo(i2.getCreatedAt()))
        .orElse(null);
  }
}
