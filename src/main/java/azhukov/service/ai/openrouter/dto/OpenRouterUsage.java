package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для использования токенов в ответе OpenRouter API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterUsage {

  /** Количество токенов в промпте */
  @JsonProperty("prompt_tokens")
  private Integer promptTokens;

  /** Количество токенов в завершении */
  @JsonProperty("completion_tokens")
  private Integer completionTokens;

  /** Общее количество токенов */
  @JsonProperty("total_tokens")
  private Integer totalTokens;
}
