package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для ответа создания агента в ElevenLabs API */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentResponse {

  @JsonProperty("agent_id")
  private String agentId;

  @JsonProperty("status")
  private String status;

  @JsonProperty("message")
  private String message;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("prompt")
  private String prompt;

  @JsonProperty("voice_id")
  private String voiceId;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;
}
