package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Сущность собеседования в системе. Связывает кандидата с вакансией и содержит результаты. */
@Entity
@Table(
    name = "interviews",
    indexes = {
      @Index(name = "idx_interviews_candidate_id", columnList = "candidate_id"),
      @Index(name = "idx_interviews_position_id", columnList = "position_id"),
      @Index(name = "idx_interviews_status", columnList = "status"),
      @Index(name = "idx_interviews_started_at", columnList = "started_at"),
      @Index(name = "idx_interviews_created_at", columnList = "created_at")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"candidate", "position", "answers"})
public class Interview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "candidate_id", nullable = false)
  @NotNull(message = "Кандидат обязателен")
  private Candidate candidate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "position_id", nullable = false)
  @NotNull(message = "Вакансия обязательна")
  private Position position;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Статус собеседования обязателен")
  @Builder.Default
  private Status status = Status.NOT_STARTED;

  @Enumerated(EnumType.STRING)
  @Column(name = "result")
  private Result result;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "ai_score")
  private Double aiScore; // Оценка от 0 до 100

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  // Новые поля для голосового интервью
  @Column(name = "voice_session_id")
  private String voiceSessionId; // ID сессии в ElevenLabs

  @Column(name = "voice_agent_id")
  private String voiceAgentId; // ID агента в ElevenLabs

  @Column(name = "voice_enabled")
  @Builder.Default
  private Boolean voiceEnabled = false; // Включено ли голосовое интервью

  @Column(name = "voice_language")
  private String voiceLanguage; // Язык голосового интервью

  @Column(name = "voice_voice_id")
  private String voiceVoiceId; // ID голоса в ElevenLabs

  @Column(name = "voice_started_at")
  private LocalDateTime voiceStartedAt; // Время начала голосовой сессии

  @Column(name = "voice_finished_at")
  private LocalDateTime voiceFinishedAt; // Время завершения голосовой сессии

  @Column(name = "voice_total_duration")
  private Long voiceTotalDuration; // Общая длительность голосового интервью в секундах

  @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<InterviewAnswer> answers = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Статусы собеседования */
  public enum Status {
    NOT_STARTED, // Не начато
    IN_PROGRESS, // В процессе
    FINISHED // Завершено
  }

  /** Результаты собеседования */
  public enum Result {
    SUCCESSFUL, // Успешно
    UNSUCCESSFUL, // Неуспешно
    ERROR // Ошибка при оценке
  }

  /** Начинает собеседование */
  public void start() {
    this.status = Status.IN_PROGRESS;
    this.startedAt = LocalDateTime.now();
  }

  /** Завершает собеседование */
  public void finish(Result result) {
    this.status = Status.FINISHED;
    this.result = result;
    this.finishedAt = LocalDateTime.now();
  }

  /** Добавляет ответ на вопрос */
  public void addAnswer(InterviewAnswer answer) {
    answers.add(answer);
    answer.setInterview(this);
  }

  /** Получает длительность собеседования в минутах */
  public Long getDurationMinutes() {
    if (startedAt == null || finishedAt == null) {
      return null;
    }
    return java.time.Duration.between(startedAt, finishedAt).toMinutes();
  }

  /** Проверяет, завершено ли собеседование */
  public boolean isFinished() {
    return status == Status.FINISHED;
  }

  /** Проверяет, успешно ли прошло собеседование */
  public boolean isSuccessful() {
    return result == Result.SUCCESSFUL;
  }

  /** Получает количество ответов */
  public int getAnswersCount() {
    return answers.size();
  }

  /** Получает среднюю оценку по ответам */
  public Double getAverageAnswerScore() {
    if (answers.isEmpty()) {
      return null;
    }
    return answers.stream()
        .mapToDouble(answer -> answer.getScore() != null ? answer.getScore() : 0.0)
        .average()
        .orElse(0.0);
  }
}
