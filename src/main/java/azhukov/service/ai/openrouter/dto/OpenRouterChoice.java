package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для выбора в ответе OpenRouter API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterChoice {

  /** Индекс выбора */
  private Integer index;

  /** Сообщение */
  private OpenRouterMessage message;

  /** Причина завершения */
  @JsonProperty("finish_reason")
  private String finishReason;

  /** Логит probs (опционально) */
  @JsonProperty("logprobs")
  private Object logprobs;
}
