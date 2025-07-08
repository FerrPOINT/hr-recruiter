package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/** DTO для запроса создания голосовой сессии в ElevenLabs Conversational AI */
@Data
@Builder
public class VoiceSessionRequest {

  /** ID агента */
  @JsonProperty("agent_id")
  private String agentId;

  /** ID голоса */
  @JsonProperty("voice_id")
  private String voiceId;

  /** Язык сессии */
  @JsonProperty("language")
  private String language;

  /** Промпт для агента */
  @JsonProperty("prompt")
  private String prompt;

  /** Настройки голоса */
  @JsonProperty("voice_settings")
  private VoiceSettings voiceSettings;

  /** Настройки сессии */
  @JsonProperty("session_settings")
  private SessionSettings sessionSettings;

  /** Настройки инструментов */
  @JsonProperty("tools")
  private Tool[] tools;

  @Data
  @Builder
  public static class VoiceSettings {
    @JsonProperty("stability")
    private Double stability = 0.5;

    @JsonProperty("similarity_boost")
    private Double similarityBoost = 0.75;

    @JsonProperty("style")
    private Double style = 0.0;

    @JsonProperty("use_speaker_boost")
    private Boolean useSpeakerBoost = true;
  }

  @Data
  @Builder
  public static class SessionSettings {
    @JsonProperty("max_duration_minutes")
    private Integer maxDurationMinutes = 60;

    @JsonProperty("response_timeout_seconds")
    private Integer responseTimeoutSeconds = 30;

    @JsonProperty("enable_emotion_detection")
    private Boolean enableEmotionDetection = true;

    @JsonProperty("enable_speaker_detection")
    private Boolean enableSpeakerDetection = false;

    @JsonProperty("enable_timestamps")
    private Boolean enableTimestamps = true;

    @JsonProperty("enable_audio_quality_analysis")
    private Boolean enableAudioQualityAnalysis = true;
  }

  @Data
  @Builder
  public static class Tool {
    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private Object parameters;

    @JsonProperty("webhook_url")
    private String webhookUrl;
  }
}
