package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/** DTO для сообщений в голосовой сессии ElevenLabs Conversational AI */
@Data
@Builder
public class VoiceMessage {

  /** Тип сообщения */
  @JsonProperty("type")
  private MessageType type;

  /** ID сессии */
  @JsonProperty("session_id")
  private String sessionId;

  /** ID вопроса (если применимо) */
  @JsonProperty("question_id")
  private Long questionId;

  /** Текст сообщения */
  @JsonProperty("text")
  private String text;

  /** Аудио данные (base64) */
  @JsonProperty("audio")
  private String audio;

  /** Временная метка */
  @JsonProperty("timestamp")
  private Long timestamp;

  /** Уверенность в распознавании (0.0-1.0) */
  @JsonProperty("confidence")
  private Double confidence;

  /** Эмоция (если определена) */
  @JsonProperty("emotion")
  private String emotion;

  /** ID говорящего */
  @JsonProperty("speaker_id")
  private String speakerId;

  /** Качество аудио (0.0-1.0) */
  @JsonProperty("audio_quality")
  private Double audioQuality;

  /** Длительность в миллисекундах */
  @JsonProperty("duration_ms")
  private Long durationMs;

  /** Метаданные */
  @JsonProperty("metadata")
  private Object metadata;

  /** Ошибка (если есть) */
  @JsonProperty("error")
  private String error;

  public enum MessageType {
    /** Агент задает вопрос */
    AGENT_QUESTION,

    /** Кандидат отвечает */
    CANDIDATE_ANSWER,

    /** Системное сообщение */
    SYSTEM_MESSAGE,

    /** Ошибка */
    ERROR,

    /** Завершение сессии */
    SESSION_END,

    /** Начало сессии */
    SESSION_START,

    /** Ожидание ответа */
    WAITING_FOR_ANSWER,

    /** Обработка аудио */
    PROCESSING_AUDIO
  }
}
