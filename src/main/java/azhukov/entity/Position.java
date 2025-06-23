package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** JPA сущность вакансии. Использует JSON для хранения массивов и современные типы данных. */
@Entity
@Table(
    name = "positions",
    indexes = {
      @Index(name = "idx_positions_status", columnList = "status"),
      @Index(name = "idx_positions_created_by", columnList = "created_by"),
      @Index(name = "idx_positions_created_at", columnList = "created_at"),
      @Index(name = "idx_positions_company", columnList = "company")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"questions", "candidates", "interviews"})
public class Position {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Название вакансии обязательно")
  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 100)
  private String company;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Статус вакансии обязателен")
  @Builder.Default
  private Status status = Status.ACTIVE;

  @Column(name = "public_link")
  private String publicLink;

  @ElementCollection
  @CollectionTable(name = "position_topics", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "topic", length = 100)
  @Builder.Default
  private List<String> topics = new ArrayList<>();

  @Column(name = "min_score")
  private Double minScore;

  @Column(name = "avg_score")
  private Double avgScore;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "position_team",
      joinColumns = @JoinColumn(name = "position_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  @Builder.Default
  private List<UserEntity> team = new ArrayList<>();

  @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Question> questions = new ArrayList<>();

  @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Candidate> candidates = new ArrayList<>();

  @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Interview> interviews = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private UserEntity createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Статусы вакансии */
  public enum Status {
    ACTIVE, // Активная
    PAUSED, // Приостановлена
    ARCHIVED // Архивная
  }

  /** Добавляет вопрос к вакансии */
  public void addQuestion(Question question) {
    questions.add(question);
    question.setPosition(this);
  }

  /** Удаляет вопрос из вакансии */
  public void removeQuestion(Question question) {
    questions.remove(question);
    question.setPosition(null);
  }

  /** Добавляет кандидата к вакансии */
  public void addCandidate(Candidate candidate) {
    candidates.add(candidate);
    candidate.setPosition(this);
  }

  /** Удаляет кандидата из вакансии */
  public void removeCandidate(Candidate candidate) {
    candidates.remove(candidate);
    candidate.setPosition(null);
  }

  /** Предварительная обработка перед сохранением. */
  @PrePersist
  @PreUpdate
  public void prePersist() {
    if (title != null) {
      title = title.trim();
    }
    if (description != null) {
      description = description.trim();
    }
    if (status == null) {
      status = Status.ACTIVE;
    }
    if (minScore != null && (minScore < 0 || minScore > 10)) {
      throw new IllegalArgumentException("Min score must be between 0 and 10");
    }
  }
}
