package azhukov.service.ai.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа от Claude API. Следует принципу Single Responsibility (SOLID) - отвечает только за
 * структуру ответа.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeResponse {

  /** ID ответа */
  @JsonProperty("id")
  private String id;

  /** Тип ответа */
  @JsonProperty("type")
  private String type;

  /** Роль ответа */
  @JsonProperty("role")
  private String role;

  /** Содержимое ответа */
  @JsonProperty("content")
  private List<ClaudeContent> content;

  /** Модель, использованная для генерации */
  @JsonProperty("model")
  private String model;

  /** Информация об использовании токенов */
  @JsonProperty("usage")
  private ClaudeUsage usage;

  /** Время завершения запроса */
  @JsonProperty("stop_reason")
  private String stopReason;

  /** Стоп-последовательность (если была использована) */
  @JsonProperty("stop_sequence")
  private String stopSequence;

  /**
   * Проверяет, содержит ли ответ текстовое содержимое.
   *
   * @return true если есть текстовое содержимое
   */
  public boolean hasTextContent() {
    return content != null && !content.isEmpty() && content.get(0).getText() != null;
  }

  /**
   * Получает текстовое содержимое ответа.
   *
   * @return Текст ответа или null
   */
  public String getTextContent() {
    if (hasTextContent()) {
      return content.get(0).getText();
    }
    return null;
  }

  /**
   * Проверяет, был ли ответ успешным.
   *
   * @return true если ответ успешен
   */
  public boolean isSuccessful() {
    return "message".equals(type) && hasTextContent();
  }
}
