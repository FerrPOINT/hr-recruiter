package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для ответа от OpenRouter API. Использует OpenAI-совместимый формат. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterResponse {

  /** ID ответа */
  private String id;

  /** Объект типа */
  private String object;

  /** Время создания */
  private Long created;

  /** Модель */
  private String model;

  /** Выборы (ответы) */
  private List<OpenRouterChoice> choices;

  /** Использование токенов */
  private OpenRouterUsage usage;

  /** Системные отпечатки */
  @JsonProperty("system_fingerprint")
  private String systemFingerprint;

  /** Получает первый ответ из выборов */
  public String getFirstContent() {
    if (choices != null && !choices.isEmpty()) {
      OpenRouterChoice firstChoice = choices.get(0);
      if (firstChoice.getMessage() != null) {
        return firstChoice.getMessage().getContent();
      }
    }
    return null;
  }
}
