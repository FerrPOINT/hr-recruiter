package azhukov.service;

import azhukov.model.PositionDataGenerationRequest;
import azhukov.model.PositionDataGenerationResponse;
import azhukov.model.PositionDataGenerationResponseGeneratedData;
import azhukov.service.ai.AIService;
import azhukov.service.ai.AIServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Сервис для генерации данных вакансии с помощью AI */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionDataGenerationService {

  private final AIService aiService;
  private final ObjectMapper objectMapper;

  /** Генерирует данные вакансии на основе описания */
  public PositionDataGenerationResponse generatePositionData(
      PositionDataGenerationRequest request) {
    try {
      log.info("Generating position data for description: {}", request.getDescription());

      // Формируем промпт для AI
      String prompt = buildPrompt(request);

      // Получаем ответ от AI
      String aiResponse = aiService.generateText(prompt);

      // Парсим ответ AI
      return parseAIResponse(aiResponse);

    } catch (AIServiceException e) {
      log.error("Error generating position data: {}", e.getMessage(), e);
      throw new RuntimeException("Ошибка генерации данных вакансии", e);
    }
  }

  /** Строит промпт для AI на основе запроса */
  private String buildPrompt(PositionDataGenerationRequest request) {
    StringBuilder prompt = new StringBuilder();

    prompt.append(
        "Ты HR-специалист. На основе описания вакансии сгенерируй структурированные данные. ");
    prompt.append(
        "Верни ответ в формате JSON с полями: title, topics (массив), status (ACTIVE/PAUSED/ARCHIVED), tags (массив), suggestedDescription.\n\n");

    prompt.append("Описание вакансии: ").append(request.getDescription()).append("\n\n");

    if (request.getExistingData() != null) {
      prompt.append("Существующие данные:\n");
      if (request.getExistingData().getTitle() != null) {
        prompt.append("- Название: ").append(request.getExistingData().getTitle()).append("\n");
      }
      if (request.getExistingData().getTopics() != null
          && !request.getExistingData().getTopics().isEmpty()) {
        prompt
            .append("- Темы: ")
            .append(String.join(", ", request.getExistingData().getTopics()))
            .append("\n");
      }
      if (request.getExistingData().getTags() != null
          && !request.getExistingData().getTags().isEmpty()) {
        prompt
            .append("- Теги: ")
            .append(String.join(", ", request.getExistingData().getTags()))
            .append("\n");
      }
      prompt.append("\n");
    }

    prompt.append("Требования к генерации:\n");
    prompt.append("1. Название должно быть профессиональным и понятным\n");
    prompt.append("2. Темы должны быть релевантными для собеседования\n");
    prompt.append("3. Теги должны отражать ключевые технологии и навыки\n");
    prompt.append("4. Описание должно быть расширенным и информативным\n");
    prompt.append("5. Статус по умолчанию ACTIVE\n\n");

    prompt.append("Верни только JSON без дополнительного текста.");

    return prompt.toString();
  }

  /** Парсит ответ AI в структурированные данные */
  private PositionDataGenerationResponse parseAIResponse(String aiResponse) {
    try {
      // Убираем лишний текст и оставляем только JSON
      String jsonResponse = extractJsonFromResponse(aiResponse);

      // Парсим JSON с помощью ObjectMapper
      return parseJsonResponse(jsonResponse);

    } catch (Exception e) {
      log.error("Error parsing AI response: {}", aiResponse, e);
      // Возвращаем дефолтные значения при ошибке парсинга
      return createDefaultResponse();
    }
  }

  /** Извлекает JSON из ответа AI */
  private String extractJsonFromResponse(String response) {
    // Ищем JSON в ответе (между { и })
    int startIndex = response.indexOf('{');
    int endIndex = response.lastIndexOf('}');

    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
      return response.substring(startIndex, endIndex + 1);
    }

    return response;
  }

  /** Парсит JSON с помощью ObjectMapper */
  private PositionDataGenerationResponse parseJsonResponse(String json) throws Exception {
    JsonNode rootNode = objectMapper.readTree(json);

    // Создаем основной ответ
    PositionDataGenerationResponse response = new PositionDataGenerationResponse();

    // Создаем объект generatedData
    PositionDataGenerationResponseGeneratedData generatedData =
        new PositionDataGenerationResponseGeneratedData();

    if (rootNode.has("title")) {
      generatedData.setTitle(rootNode.get("title").asText());
    }

    if (rootNode.has("topics")) {
      List<String> topics =
          objectMapper.convertValue(
              rootNode.get("topics"),
              objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
      generatedData.setTopics(topics);
    }

    if (rootNode.has("status")) {
      String statusStr = rootNode.get("status").asText();
      log.info("Received status from AI: {}", statusStr);
      // Пока не устанавливаем статус, так как метода setStatus нет в модели
    }

    if (rootNode.has("tags")) {
      List<String> tags =
          objectMapper.convertValue(
              rootNode.get("tags"),
              objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
      generatedData.setTags(tags);
    }

    if (rootNode.has("suggestedDescription")) {
      generatedData.setSuggestedDescription(rootNode.get("suggestedDescription").asText());
    }

    response.setGeneratedData(generatedData);
    response.setConfidence(0.85f);
    response.setMessage("Данные успешно сгенерированы");

    return response;
  }

  /** Создает дефолтный ответ при ошибке */
  private PositionDataGenerationResponse createDefaultResponse() {
    PositionDataGenerationResponse response = new PositionDataGenerationResponse();

    // Создаем дефолтные данные
    PositionDataGenerationResponseGeneratedData generatedData =
        new PositionDataGenerationResponseGeneratedData();
    generatedData.setTitle("Название вакансии");
    generatedData.setTopics(List.of("Основные темы"));
    generatedData.setTags(List.of("Теги"));
    generatedData.setSuggestedDescription("Описание вакансии");

    response.setGeneratedData(generatedData);
    response.setConfidence(0.5f);
    response.setMessage("Ошибка парсинга ответа AI");

    return response;
  }
}
