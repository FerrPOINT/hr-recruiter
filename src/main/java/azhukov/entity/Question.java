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

/**
 * Сущность вопроса для собеседования. Может быть различных типов: текстовый, аудио, видео, выбор.
 */
@Entity
@Table(
    name = "questions",
    indexes = {
      @Index(name = "idx_questions_position_id", columnList = "position_id"),
      @Index(name = "idx_questions_type", columnList = "type"),
      @Index(name = "idx_questions_order", columnList = "position_id, question_order")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"position", "answers"})
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "position_id", nullable = false)
  @NotNull(message = "Вакансия обязательна")
  private Position position;

  @NotBlank(message = "Текст вопроса обязателен")
  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Тип вопроса обязателен")
  @Builder.Default
  private Type type = Type.TEXT;

  @Column(name = "question_order", nullable = false)
  @NotNull(message = "Порядок вопроса обязателен")
  private Integer order;

  @Column(name = "is_required")
  @Builder.Default
  private boolean required = true;

  @Column(name = "max_duration")
  private Integer maxDuration; // в секундах для аудио/видео вопросов

  @ElementCollection
  @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
  @Column(name = "option_text", length = 500)
  @Builder.Default
  private List<String> options = new ArrayList<>(); // для вопросов типа CHOICE

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<InterviewAnswer> answers = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Типы вопросов */
  public enum Type {
    TEXT, // Текстовый ответ
    AUDIO, // Аудио ответ
    VIDEO, // Видео ответ
    CHOICE // Выбор из вариантов
  }

  /** Добавляет вариант ответа для вопроса типа CHOICE */
  public void addOption(String option) {
    if (type == Type.CHOICE) {
      options.add(option);
    }
  }

  /** Проверяет, является ли вопрос обязательным */
  public boolean isRequired() {
    return required;
  }

  /** Получает максимальную длительность ответа в секундах */
  public Integer getMaxDurationSeconds() {
    return maxDuration != null
        ? maxDuration
        : (type == Type.AUDIO ? 120 : type == Type.VIDEO ? 300 : null);
  }
}
