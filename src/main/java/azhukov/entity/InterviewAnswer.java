package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** Сущность ответа на вопрос собеседования. Содержит ответ кандидата на конкретный вопрос. */
@Entity
@Table(
    name = "interview_answers",
    indexes = {
      @Index(name = "idx_interview_answers_interview_id", columnList = "interview_id"),
      @Index(name = "idx_interview_answers_question_id", columnList = "question_id"),
      @Index(name = "idx_interview_answers_created_at", columnList = "created_at")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"interview", "question"})
public class InterviewAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interview_id", nullable = false)
  @NotNull(message = "Собеседование обязательно")
  private Interview interview;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", nullable = false)
  @NotNull(message = "Вопрос обязателен")
  private Question question;

  @Column(name = "answer_text", columnDefinition = "TEXT")
  private String answerText;

  @Column(name = "audio_url")
  private String audioUrl;

  @Column(name = "video_url")
  private String videoUrl;

  @Column(name = "transcript", columnDefinition = "TEXT")
  private String transcript;

  @Column(name = "raw_transcription", columnDefinition = "TEXT")
  private String rawTranscription; // Сырой транскрибированный текст от Whisper

  @Column(name = "formatted_transcription", columnDefinition = "TEXT")
  private String formattedTranscription; // Отформатированный текст от Claude

  @Column(name = "score")
  private Double score; // Оценка от 0 до 100

  @Column(name = "feedback", columnDefinition = "TEXT")
  private String feedback;

  @Column(name = "duration_seconds")
  private Integer durationSeconds; // Длительность ответа в секундах

  @Column(name = "is_correct")
  private Boolean isCorrect; // Для вопросов типа CHOICE

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Устанавливает текстовый ответ */
  public void setTextAnswer(String text) {
    this.answerText = text;
  }

  /** Устанавливает аудио ответ */
  public void setAudioAnswer(String audioUrl, String transcript) {
    this.audioUrl = audioUrl;
    this.transcript = transcript;
  }

  /** Устанавливает видео ответ */
  public void setVideoAnswer(String videoUrl, String transcript) {
    this.videoUrl = videoUrl;
    this.transcript = transcript;
  }

  /** Устанавливает оценку ответа */
  public void setScore(Double score) {
    if (score != null && (score < 0 || score > 100)) {
      throw new IllegalArgumentException("Оценка должна быть от 0 до 100");
    }
    this.score = score;
  }

  /** Проверяет, является ли ответ полным */
  public boolean isComplete() {
    return answerText != null || audioUrl != null || videoUrl != null;
  }

  /** Получает тип ответа */
  public String getAnswerType() {
    if (videoUrl != null) return "VIDEO";
    if (audioUrl != null) return "AUDIO";
    if (answerText != null) return "TEXT";
    return "NONE";
  }

  /** Получает длительность ответа в формате MM:SS */
  public String getFormattedDuration() {
    if (durationSeconds == null) return null;
    int minutes = durationSeconds / 60;
    int seconds = durationSeconds % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }
}
