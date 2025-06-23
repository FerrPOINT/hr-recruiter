package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса к Claude API. Следует принципу Single Responsibility (SOLID) - отвечает только за
 * структуру запроса.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeRequest {

  /** Модель Claude для использования */
  @JsonProperty("model")
  private String model;

  /** Максимальное количество токенов в ответе */
  @JsonProperty("max_tokens")
  private Integer maxTokens;

  /**
   * Температура генерации (0.0 - 1.0) 0.0 = детерминированный ответ, 1.0 = максимальная
   * креативность
   */
  @JsonProperty("temperature")
  private Double temperature;

  /** Сообщения для контекста */
  @JsonProperty("messages")
  private List<ClaudeMessage> messages;

  /** Системный промпт (опционально) */
  @JsonProperty("system")
  private String system;

  /** Стоп-слова (опционально) */
  @JsonProperty("stop_sequences")
  private List<String> stopSequences;

  /** Топ-p параметр для ядерной выборки (опционально) */
  @JsonProperty("top_p")
  private Double topP;

  /** Топ-k параметр для ядерной выборки (опционально) */
  @JsonProperty("top_k")
  private Integer topK;

  /** Метаданные запроса (опционально) */
  @JsonProperty("metadata")
  private ClaudeMetadata metadata;

  /**
   * Создает простой запрос с одним сообщением.
   *
   * @param model Модель Claude
   * @param message Текст сообщения
   * @param maxTokens Максимальное количество токенов
   * @return ClaudeRequest
   */
  public static ClaudeRequest simpleRequest(String model, String message, Integer maxTokens) {
    return ClaudeRequest.builder()
        .model(model)
        .maxTokens(maxTokens)
        .temperature(0.7)
        .messages(List.of(ClaudeMessage.builder().role("user").content(message).build()))
        .build();
  }

  /**
   * Создает запрос с системным промптом.
   *
   * @param model Модель Claude
   * @param systemPrompt Системный промпт
   * @param userMessage Пользовательское сообщение
   * @param maxTokens Максимальное количество токенов
   * @return ClaudeRequest
   */
  public static ClaudeRequest withSystemPrompt(
      String model, String systemPrompt, String userMessage, Integer maxTokens) {
    return ClaudeRequest.builder()
        .model(model)
        .maxTokens(maxTokens)
        .temperature(0.7)
        .system(systemPrompt)
        .messages(List.of(ClaudeMessage.builder().role("user").content(userMessage).build()))
        .build();
  }

  /**
   * Создает запрос с контекстом (историей сообщений).
   *
   * @param model Модель Claude
   * @param messages История сообщений
   * @param maxTokens Максимальное количество токенов
   * @return ClaudeRequest
   */
  public static ClaudeRequest withContext(
      String model, List<ClaudeMessage> messages, Integer maxTokens) {
    return ClaudeRequest.builder()
        .model(model)
        .maxTokens(maxTokens)
        .temperature(0.7)
        .messages(messages)
        .build();
  }
}
