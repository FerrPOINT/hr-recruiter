package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO для запроса к OpenRouter API. OpenRouter использует OpenAI-совместимый формат запросов. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenRouterRequest {

  /** Модель для использования */
  private String model;

  /** Сообщения в формате OpenAI */
  private List<OpenRouterMessage> messages;

  /** Максимальное количество токенов */
  @JsonProperty("max_tokens")
  private Integer maxTokens;

  /** Температура генерации (0.0 - 2.0) */
  private Double temperature;

  /** Top-p параметр */
  @JsonProperty("top_p")
  private Double topP;

  /** Количество ответов */
  private Integer n;

  /** Стриминг ответа */
  private Boolean stream;

  /** Стоп-последовательности */
  private List<String> stop;

  /** Присутствие penalty */
  @JsonProperty("presence_penalty")
  private Double presencePenalty;

  /** Частота penalty */
  @JsonProperty("frequency_penalty")
  private Double frequencyPenalty;

  /** Логит bias */
  @JsonProperty("logit_bias")
  private Object logitBias;

  /** Пользователь */
  private String user;

  /** Создает простой запрос для генерации текста */
  public static OpenRouterRequest simpleRequest(String model, String prompt, Integer maxTokens) {
    return OpenRouterRequest.builder()
        .model(model)
        .messages(List.of(OpenRouterMessage.userMessage(prompt)))
        .maxTokens(maxTokens)
        .temperature(0.7)
        .build();
  }
}
