package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для запроса создания агента в ElevenLabs Conversational AI API */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

  @JsonProperty("conversation_config")
  private ConversationConfig conversationConfig;

  @JsonProperty("platform_settings")
  private PlatformSettings platformSettings;

  @JsonProperty("name")
  private String name;

  @JsonProperty("tags")
  private List<String> tags;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConversationConfig {
    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("voice_id")
    private String voiceId;

    @JsonProperty("language")
    private String language;

    @JsonProperty("personality")
    private String personality;

    @JsonProperty("tools")
    private List<String> tools;

    @JsonProperty("webhook_url")
    private String webhookUrl;

    @JsonProperty("voice_settings")
    private VoiceSettings voiceSettings;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlatformSettings {
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

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VoiceSettings {
    @JsonProperty("stability")
    private Double stability;

    @JsonProperty("similarity_boost")
    private Double similarityBoost;

    @JsonProperty("style")
    private Double style;

    @JsonProperty("use_speaker_boost")
    private Boolean useSpeakerBoost;
  }
}
