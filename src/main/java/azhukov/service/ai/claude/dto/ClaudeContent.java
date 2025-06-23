package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для содержимого ответа Claude API. Следует принципу Single Responsibility (SOLID) - отвечает
 * только за структуру содержимого.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeContent {

  /** Тип содержимого */
  @JsonProperty("type")
  private String type;

  /** Текстовое содержимое */
  @JsonProperty("text")
  private String text;

  /**
   * Проверяет, является ли содержимое текстовым.
   *
   * @return true если содержимое текстовое
   */
  public boolean isText() {
    return "text".equals(type);
  }

  /**
   * Проверяет, есть ли текстовое содержимое.
   *
   * @return true если есть текст
   */
  public boolean hasText() {
    return text != null && !text.trim().isEmpty();
  }
}
