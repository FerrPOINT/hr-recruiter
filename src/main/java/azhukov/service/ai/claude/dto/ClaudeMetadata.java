package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для метаданных Claude API. Следует принципу Single Responsibility (SOLID) - отвечает только
 * за структуру метаданных.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeMetadata {

  /** ID пользователя (опционально) */
  @JsonProperty("user_id")
  private String userId;

  /** Дополнительные свойства метаданных */
  @JsonProperty("additional_properties")
  private Map<String, Object> additionalProperties;

  /**
   * Создает простые метаданные с ID пользователя.
   *
   * @param userId ID пользователя
   * @return ClaudeMetadata
   */
  public static ClaudeMetadata withUserId(String userId) {
    return ClaudeMetadata.builder().userId(userId).build();
  }

  /**
   * Создает метаданные с дополнительными свойствами.
   *
   * @param userId ID пользователя
   * @param additionalProperties Дополнительные свойства
   * @return ClaudeMetadata
   */
  public static ClaudeMetadata withProperties(
      String userId, Map<String, Object> additionalProperties) {
    return ClaudeMetadata.builder()
        .userId(userId)
        .additionalProperties(additionalProperties)
        .build();
  }
}
