package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** DTO для ответа создания голосовой сессии в ElevenLabs Conversational AI */
@Data
public class VoiceSessionResponse {

  /** ID сессии */
  @JsonProperty("session_id")
  private String sessionId;

  /** ID агента */
  @JsonProperty("agent_id")
  private String agentId;

  /** ID голоса */
  @JsonProperty("voice_id")
  private String voiceId;

  /** Статус сессии */
  @JsonProperty("status")
  private String status;

  /** URL для подключения к WebSocket */
  @JsonProperty("websocket_url")
  private String websocketUrl;

  /** URL для получения следующего вопроса */
  @JsonProperty("next_question_url")
  private String nextQuestionUrl;

  /** URL для отправки ответа */
  @JsonProperty("answer_url")
  private String answerUrl;

  /** URL для завершения сессии */
  @JsonProperty("end_session_url")
  private String endSessionUrl;

  /** Настройки сессии */
  @JsonProperty("settings")
  private SessionSettings settings;

  /** Ошибка (если есть) */
  @JsonProperty("error")
  private String error;

  @Data
  public static class SessionSettings {
    @JsonProperty("max_duration_minutes")
    private Integer maxDurationMinutes;

    @JsonProperty("response_timeout_seconds")
    private Integer responseTimeoutSeconds;

    @JsonProperty("enable_emotion_detection")
    private Boolean enableEmotionDetection;

    @JsonProperty("enable_speaker_detection")
    private Boolean enableSpeakerDetection;

    @JsonProperty("enable_timestamps")
    private Boolean enableTimestamps;

    @JsonProperty("enable_audio_quality_analysis")
    private Boolean enableAudioQualityAnalysis;
  }
}
