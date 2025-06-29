package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для сообщений в OpenRouter API. Использует OpenAI-совместимый формат. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenRouterMessage {

  /** Роль сообщения (system, user, assistant) */
  private String role;

  /** Содержимое сообщения */
  private String content;

  /** Имя (опционально) */
  private String name;

  /** Создает системное сообщение */
  public static OpenRouterMessage systemMessage(String content) {
    return OpenRouterMessage.builder().role("system").content(content).build();
  }

  /** Создает пользовательское сообщение */
  public static OpenRouterMessage userMessage(String content) {
    return OpenRouterMessage.builder().role("user").content(content).build();
  }

  /** Создает сообщение ассистента */
  public static OpenRouterMessage assistantMessage(String content) {
    return OpenRouterMessage.builder().role("assistant").content(content).build();
  }
}
