package azhukov.service.ai.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** DTO для парсинга JSON ответа от AI при генерации вакансии. */
@Data
public class PositionGenerationResponse {

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  @JsonProperty("topics")
  private List<String> topics;

  @JsonProperty("level")
  private String level;

  @JsonProperty("questions")
  private List<Question> questions;

  @Data
  public static class Question {
    @JsonProperty("text")
    private String text;

    @JsonProperty("type")
    private String type;

    @JsonProperty("order")
    private Integer order;
  }
}
