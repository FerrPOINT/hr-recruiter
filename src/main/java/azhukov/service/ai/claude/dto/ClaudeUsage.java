package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для информации об использовании токенов Claude API. Следует принципу Single Responsibility
 * (SOLID) - отвечает только за структуру использования токенов.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeUsage {

  /** Количество входных токенов */
  @JsonProperty("input_tokens")
  private Integer inputTokens;

  /** Количество выходных токенов */
  @JsonProperty("output_tokens")
  private Integer outputTokens;

  /** Общее количество токенов */
  @JsonProperty("total_tokens")
  private Integer totalTokens;

  /**
   * Получает общее количество токенов.
   *
   * @return Общее количество токенов
   */
  public Integer getTotalTokens() {
    if (totalTokens != null) {
      return totalTokens;
    }

    int input = inputTokens != null ? inputTokens : 0;
    int output = outputTokens != null ? outputTokens : 0;
    return input + output;
  }

  /**
   * Проверяет, есть ли информация об использовании токенов.
   *
   * @return true если есть информация
   */
  public boolean hasUsageInfo() {
    return inputTokens != null || outputTokens != null || totalTokens != null;
  }

  /**
   * Вычисляет стоимость запроса (примерная оценка).
   *
   * @return Стоимость в долларах
   */
  public double calculateCost() {
    // Примерные цены для Claude 3 Sonnet (2024)
    double inputCostPer1k = 0.003; // $3 per 1M tokens
    double outputCostPer1k = 0.015; // $15 per 1M tokens

    int input = inputTokens != null ? inputTokens : 0;
    int output = outputTokens != null ? outputTokens : 0;

    return (input * inputCostPer1k / 1000.0) + (output * outputCostPer1k / 1000.0);
  }
}
