package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для метаданных сообщений Claude API. Следует принципу Single Responsibility (SOLID) -
 * отвечает только за структуру метаданных сообщения.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeMessageMetadata {

  /** Тип сообщения */
  @JsonProperty("message_type")
  private String messageType;

  /** Дополнительные свойства метаданных */
  @JsonProperty("additional_properties")
  private Map<String, Object> additionalProperties;

  /**
   * Создает метаданные с типом сообщения.
   *
   * @param messageType Тип сообщения
   * @return ClaudeMessageMetadata
   */
  public static ClaudeMessageMetadata withType(String messageType) {
    return ClaudeMessageMetadata.builder().messageType(messageType).build();
  }

  /**
   * Создает метаданные с дополнительными свойствами.
   *
   * @param messageType Тип сообщения
   * @param additionalProperties Дополнительные свойства
   * @return ClaudeMessageMetadata
   */
  public static ClaudeMessageMetadata withProperties(
      String messageType, Map<String, Object> additionalProperties) {
    return ClaudeMessageMetadata.builder()
        .messageType(messageType)
        .additionalProperties(additionalProperties)
        .build();
  }
}
