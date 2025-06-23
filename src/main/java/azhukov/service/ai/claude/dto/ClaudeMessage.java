package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для сообщений Claude API. Следует принципу Single Responsibility (SOLID) - отвечает только за
 * структуру сообщения.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeMessage {

  /** Роль отправителя сообщения */
  @JsonProperty("role")
  private String role;

  /** Содержимое сообщения */
  @JsonProperty("content")
  private String content;

  /** Тип содержимого (опционально) */
  @JsonProperty("type")
  private String type;

  /** Метаданные сообщения (опционально) */
  @JsonProperty("metadata")
  private ClaudeMessageMetadata metadata;

  /**
   * Создает пользовательское сообщение.
   *
   * @param content Содержимое сообщения
   * @return ClaudeMessage
   */
  public static ClaudeMessage userMessage(String content) {
    return ClaudeMessage.builder().role("user").content(content).type("text").build();
  }

  /**
   * Создает сообщение ассистента.
   *
   * @param content Содержимое сообщения
   * @return ClaudeMessage
   */
  public static ClaudeMessage assistantMessage(String content) {
    return ClaudeMessage.builder().role("assistant").content(content).type("text").build();
  }

  /**
   * Создает системное сообщение.
   *
   * @param content Содержимое сообщения
   * @return ClaudeMessage
   */
  public static ClaudeMessage systemMessage(String content) {
    return ClaudeMessage.builder().role("system").content(content).type("text").build();
  }

  /**
   * Проверяет, является ли сообщение пользовательским.
   *
   * @return true если сообщение от пользователя
   */
  public boolean isUserMessage() {
    return "user".equals(role);
  }

  /**
   * Проверяет, является ли сообщение от ассистента.
   *
   * @return true если сообщение от ассистента
   */
  public boolean isAssistantMessage() {
    return "assistant".equals(role);
  }

  /**
   * Проверяет, является ли сообщение системным.
   *
   * @return true если системное сообщение
   */
  public boolean isSystemMessage() {
    return "system".equals(role);
  }
}
