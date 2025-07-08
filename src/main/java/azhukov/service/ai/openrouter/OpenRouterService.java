package azhukov.service.ai.openrouter;

import azhukov.config.ApplicationProperties;
import azhukov.config.OpenRouterConfig;
import azhukov.entity.Position;
import azhukov.service.ai.AIService;
import azhukov.service.ai.AIServiceException;
import azhukov.service.ai.AIUsageStats;
import azhukov.service.ai.openrouter.dto.OpenRouterRequest;
import azhukov.service.ai.openrouter.dto.OpenRouterResponse;
import azhukov.service.ai.openrouter.dto.OpenRouterUsage;
import azhukov.service.ai.openrouter.dto.PositionGenerationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Сервис для работы с OpenRouter API. OpenRouter предоставляет унифицированный интерфейс для
 * множества LLM моделей.
 *
 * @author AI Team
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class OpenRouterService implements AIService {

  // Конфигурация
  private final OpenRouterConfig openRouterConfig;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  // Статистика использования (Thread-safe)
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong successfulRequests = new AtomicLong(0);
  private final AtomicLong failedRequests = new AtomicLong(0);
  private final AtomicLong totalTokens = new AtomicLong(0);
  private final AtomicReference<LocalDateTime> firstRequestTime = new AtomicReference<>();
  private final AtomicReference<LocalDateTime> lastRequestTime = new AtomicReference<>();
  private final Map<String, Long> modelUsage = new ConcurrentHashMap<>();

  // Константы
  private static final String SERVICE_NAME = "OpenRouter AI";
  private static final String DEFAULT_ROLE = "user";

  /** {@inheritDoc} */
  @Override
  public String generateText(String prompt) throws AIServiceException {
    log.debug("Generating text with OpenRouter, prompt length: {}", prompt.length());

    try {
      validateApiKey();
      updateRequestStats();

      OpenRouterRequest request =
          OpenRouterRequest.simpleRequest(
              openRouterConfig.getModel(), prompt, openRouterConfig.getMaxTokens());
      OpenRouterResponse response = sendRequest(request);

      updateSuccessStats(response.getUsage());
      return extractContent(response);

    } catch (Exception e) {
      updateFailureStats();
      throw createAIServiceException("Failed to generate text", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<String> generateTextList(String prompt, int count) throws AIServiceException {
    log.debug("Generating {} text items with OpenRouter", count);

    try {
      validateApiKey();
      updateRequestStats();

      // Модифицируем промпт для генерации списка
      String listPrompt =
          String.format("%s\n\nВерни ровно %d элементов, каждый с новой строки:", prompt, count);

      OpenRouterRequest request =
          OpenRouterRequest.simpleRequest(
              openRouterConfig.getModel(), listPrompt, openRouterConfig.getMaxTokens());
      OpenRouterResponse response = sendRequest(request);

      updateSuccessStats(response.getUsage());
      String content = extractContent(response);

      return parseListResponse(content, count);

    } catch (Exception e) {
      updateFailureStats();
      throw createAIServiceException("Failed to generate text list", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isAvailable() {
    try {
      String testPrompt = "Ответь одним словом: 'работает'";
      String response = generateText(testPrompt);
      return response != null && !response.contains("Ошибка") && !response.contains("демо");
    } catch (Exception e) {
      log.error("OpenRouter service availability check failed", e);
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getModelInfo() {
    return String.format("OpenRouter %s", openRouterConfig.getModel());
  }

  /** {@inheritDoc} */
  @Override
  public AIUsageStats getUsageStats() {
    return AIUsageStats.builder()
        .serviceName(SERVICE_NAME)
        .totalRequests(totalRequests.get())
        .successfulRequests(successfulRequests.get())
        .failedRequests(failedRequests.get())
        .totalTokens(totalTokens.get())
        .averageResponseTimeMs(calculateAverageResponseTime())
        .lastRequestTime(lastRequestTime.get())
        .firstRequestTime(firstRequestTime.get())
        .build();
  }

  // ========== СПЕЦИАЛИЗИРОВАННЫЕ МЕТОДЫ ==========

  /**
   * Генерирует вопросы для собеседования на основе описания вакансии.
   *
   * @param jobDescription Описание вакансии
   * @param count Количество вопросов
   * @param questionTypes Типы вопросов (технические, поведенческие, ситуационные)
   * @return Список вопросов
   * @throws AIServiceException если произошла ошибка
   */
  public List<String> generateInterviewQuestions(
      String jobDescription, int count, List<String> questionTypes) throws AIServiceException {

    log.info("Generating {} interview questions for job description", count);

    String prompt = buildInterviewQuestionsPrompt(jobDescription, count, questionTypes);
    return generateTextList(prompt, count);
  }

  /**
   * Анализирует ответ кандидата и дает оценку.
   *
   * @param question Вопрос
   * @param candidateAnswer Ответ кандидата
   * @return Анализ ответа
   * @throws AIServiceException если произошла ошибка
   */
  public String analyzeCandidateAnswer(String question, String candidateAnswer)
      throws AIServiceException {
    log.info(
        "Analyzing candidate answer for question: {}",
        question.substring(0, Math.min(50, question.length())));

    String prompt = buildAnswerAnalysisPrompt(question, candidateAnswer);
    return generateText(prompt);
  }

  /**
   * Улучшает описание вакансии.
   *
   * @param originalDescription Исходное описание
   * @param improvementType Тип улучшения (структура, детализация, привлекательность)
   * @return Улучшенное описание
   * @throws AIServiceException если произошла ошибка
   */
  public String improveJobDescription(String originalDescription, String improvementType)
      throws AIServiceException {
    log.info("Improving job description with type: {}", improvementType);

    String prompt = buildJobDescriptionImprovementPrompt(originalDescription, improvementType);
    return generateText(prompt);
  }

  /** Строит промпт для генерации вакансии с минимально необходимыми параметрами. */
  private String buildPositionGenerationPrompt(
      String description, int questionsCount, String questionType) {
    return String.format(
        "Сгенерируй структуру вакансии на основе описания пользователя. ВАЖНО: используй именно то описание, которое дал пользователь, не заменяй его на стандартные вакансии.\n\n"
            + "ЯЗЫКОВЫЕ ТРЕБОВАНИЯ:\n"
            + "- ВСЕГДА отвечай на русском языке\n"
            + "- Название вакансии должно быть на русском языке\n"
            + "- Описание вакансии должно быть на русском языке\n"
            + "- Вопросы должны быть на русском языке\n"
            + "- Технические термины (Java, Python, UI, API, SQL и т.д.) могут быть на английском\n"
            + "- Теги и названия технологий могут быть на английском языке\n"
            + "- Остальной текст должен быть на русском языке\n\n"
            + "Описание пользователя: %s\n"
            + "Количество вопросов: %d\n"
            + "Тип вопросов: %s (варианты: hard, soft, mixed и т.д.)\n\n"
            + "ПРАВИЛА:\n"
            + "1. Название вакансии должно соответствовать описанию пользователя\n"
            + "2. Название вакансии должно быть не длинным, максимум 50 символов, без лишних слов\n"
            + "3. Описание вакансии должно быть основано на том, что написал пользователь\n"
            + "4. Если описание пользователя содержит мало информации, дополни его логично\n"
            + "5. Не генерируй стандартные вакансии (Java Developer, Python Developer и т.д.) если пользователь не указал это\n"
            + "6. ВСЕ тексты должны быть на русском языке\n\n"
            + "Верни только JSON следующей структуры:\n"
            + "{\n"
            + "  \"title\": \"string\",\n"
            + "  \"description\": \"string\",\n"
            + "  \"topics\": [\"string\", ...],\n"
            + "  \"level\": \"junior|middle|senior|lead\",\n"
            + "  \"questions\": [\n"
            + "    {\n"
            + "      \"text\": \"string\",\n"
            + "      \"type\": \"text|audio|choice\",\n"
            + "      \"order\": 1\n"
            + "    }\n"
            + "    // ... столько, сколько указано в questionsCount\n"
            + "  ]\n"
            + "}\n",
        description, questionsCount, questionType);
  }

  /**
   * Генерирует вакансию на основе описания пользователя. Если описание содержит мало информации или
   * бессмыслицу, генерирует хотя бы название вакансии.
   *
   * @param userDescription Описание от пользователя
   * @return Сгенерированная вакансия
   * @throws AIServiceException если произошла ошибка
   */
  public Position generatePosition(String userDescription) throws AIServiceException {
    log.info(
        "Generating position from user description: {}",
        userDescription.substring(0, Math.min(100, userDescription.length())));

    String prompt = buildPositionGenerationPrompt(userDescription, 5, "hard");
    String response = generateText(prompt);

    return parsePositionResponse(response);
  }

  /**
   * Генерирует структуру вакансии через AI и возвращает DTO ответа. Используется для нового API
   * endpoint.
   *
   * @param description Описание от пользователя
   * @param questionsCount Количество вопросов
   * @param questionType Тип вопросов
   * @return DTO с AI-ответом
   * @throws AIServiceException если произошла ошибка
   */
  public PositionGenerationResponse generatePositionWithAI(
      String description, int questionsCount, String questionType) throws AIServiceException {
    log.info(
        "Generating position with AI: description={}, questionsCount={}, questionType={}",
        description.substring(0, Math.min(100, description.length())),
        questionsCount,
        questionType);

    String prompt = buildPositionGenerationPrompt(description, questionsCount, questionType);
    String response = generateText(prompt);

    return parsePositionGenerationResponse(response);
  }

  // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

  private OpenRouterResponse sendRequest(OpenRouterRequest request) throws AIServiceException {
    try {
      // Выводим сериализованный JSON запроса
      String jsonRequest = objectMapper.writeValueAsString(request);
      System.out.println("OpenRouter request JSON: " + jsonRequest);

      HttpHeaders headers = createHeaders();
      HttpEntity<OpenRouterRequest> entity = new HttpEntity<>(request, headers);
      long start = System.currentTimeMillis();
      ResponseEntity<OpenRouterResponse> responseEntity =
          restTemplate.postForEntity(
              openRouterConfig.getApiUrl(), entity, OpenRouterResponse.class);
      long end = System.currentTimeMillis();
      log.info("OpenRouter API response time: {} ms", (end - start));
      if (responseEntity.getStatusCode() != HttpStatus.OK) {
        throw new AIServiceException(
            "OpenRouter API returned non-OK status: " + responseEntity.getStatusCode(),
            AIServiceException.ErrorType.INVALID_REQUEST,
            SERVICE_NAME);
      }
      return responseEntity.getBody();
    } catch (Exception e) {
      // Если это ошибка клиента, выводим тело ответа
      if (e instanceof org.springframework.web.client.HttpClientErrorException clientEx) {
        System.out.println("OpenRouter API error body: " + clientEx.getResponseBodyAsString());
      }
      throw createAIServiceException("OpenRouter API client error", e);
    }
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + openRouterConfig.getApiKey());
    headers.set("HTTP-Referer", "https://github.com/azhukov/hr-recruiter-back");
    headers.set("X-Title", "HR Recruiter Backend");
    return headers;
  }

  private String extractContent(OpenRouterResponse response) throws AIServiceException {
    if (response == null) {
      throw new AIServiceException(
          "Empty response from OpenRouter API",
          AIServiceException.ErrorType.API_UNAVAILABLE,
          SERVICE_NAME);
    }

    String content = response.getFirstContent();
    if (content == null || content.trim().isEmpty()) {
      throw new AIServiceException(
          "No content in OpenRouter response",
          AIServiceException.ErrorType.API_UNAVAILABLE,
          SERVICE_NAME);
    }

    return content.trim();
  }

  private List<String> parseListResponse(String content, int expectedCount) {
    return List.of(content.split("\n")).stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .limit(expectedCount)
        .toList();
  }

  private void validateApiKey() throws AIServiceException {
    if (!openRouterConfig.isApiKeyConfigured()) {
      throw new AIServiceException(
          "OpenRouter API key not configured",
          AIServiceException.ErrorType.INVALID_REQUEST,
          SERVICE_NAME);
    }
  }

  private void updateRequestStats() {
    totalRequests.incrementAndGet();
    if (firstRequestTime.get() == null) {
      firstRequestTime.set(LocalDateTime.now());
    }
    lastRequestTime.set(LocalDateTime.now());
  }

  private void updateSuccessStats(OpenRouterUsage usage) {
    successfulRequests.incrementAndGet();
    if (usage != null && usage.getTotalTokens() != null) {
      totalTokens.addAndGet(usage.getTotalTokens());
    }
  }

  private void updateFailureStats() {
    failedRequests.incrementAndGet();
  }

  private AIServiceException createAIServiceException(String message, Throwable cause) {
    AIServiceException.ErrorType errorType = determineErrorType(cause);
    return new AIServiceException(message, cause, errorType, SERVICE_NAME);
  }

  private AIServiceException.ErrorType determineErrorType(Throwable cause) {
    if (cause instanceof org.springframework.web.client.HttpClientErrorException) {
      return AIServiceException.ErrorType.INVALID_REQUEST;
    } else if (cause instanceof org.springframework.web.client.HttpServerErrorException) {
      return AIServiceException.ErrorType.API_UNAVAILABLE;
    } else if (cause instanceof org.springframework.web.client.ResourceAccessException) {
      return AIServiceException.ErrorType.NETWORK_ERROR;
    }
    return AIServiceException.ErrorType.UNKNOWN_ERROR;
  }

  private double calculateAverageResponseTime() {
    // Простая реализация - можно улучшить
    return 0.0;
  }

  // ========== ПРОМПТЫ ==========

  private String buildInterviewQuestionsPrompt(
      String jobDescription, int count, List<String> questionTypes) {
    return String.format(
        "Создай %d вопросов для собеседования на позицию с описанием:\n\n%s\n\n"
            + "ЯЗЫКОВЫЕ ТРЕБОВАНИЯ:\n"
            + "- ВСЕГДА отвечай на русском языке\n"
            + "- Вопросы должны быть на русском языке\n"
            + "- Технические термины (Java, Python, UI, API, SQL и т.д.) могут быть на английском\n"
            + "- Названия технологий и фреймворков могут быть на английском языке\n"
            + "- Остальной текст должен быть на русском языке\n\n"
            + "Типы вопросов: %s\n\n"
            + "Вопросы должны быть:\n"
            + "- Релевантными для данной позиции\n"
            + "- Разнообразными по сложности\n"
            + "- Практическими и конкретными\n"
            + "- Направленными на оценку навыков и опыта\n"
            + "- На русском языке\n\n"
            + "Верни только вопросы, каждый с новой строки, без нумерации.",
        count, jobDescription, String.join(", ", questionTypes));
  }

  private String buildAnswerAnalysisPrompt(String question, String candidateAnswer) {
    return String.format(
        "Проанализируй ответ кандидата на вопрос:\n\n"
            + "ЯЗЫКОВЫЕ ТРЕБОВАНИЯ:\n"
            + "- ВСЕГДА отвечай на русском языке\n"
            + "- Анализ должен быть на русском языке\n"
            + "- Комментарии должны быть на русском языке\n"
            + "- Рекомендации должны быть на русском языке\n"
            + "- Технические термины (Java, Python, UI, API, SQL и т.д.) могут быть на английском\n\n"
            + "Вопрос: %s\n"
            + "Ответ кандидата: %s\n\n"
            + "Дай оценку по следующим критериям:\n"
            + "1. Релевантность ответа (0-10)\n"
            + "2. Глубина знаний (0-10)\n"
            + "3. Практический опыт (0-10)\n"
            + "4. Коммуникативные навыки (0-10)\n"
            + "5. Общая оценка (0-10)\n\n"
            + "Добавь краткий комментарий к каждому критерию и общие рекомендации на русском языке.",
        question, candidateAnswer);
  }

  private String buildJobDescriptionImprovementPrompt(
      String originalDescription, String improvementType) {
    return String.format(
        "Улучши описание вакансии:\n\n%s\n\n"
            + "Тип улучшения: %s\n\n"
            + "Сделай описание более привлекательным, структурированным и информативным.",
        originalDescription, improvementType);
  }

  /** Парсит ответ AI в сущность Position с дефолтными значениями для бизнес-полей. */
  private Position parsePositionResponse(String response) {
    try {
      String jsonResponse = extractJsonFromResponse(response);
      if (jsonResponse == null) {
        throw new RuntimeException("No JSON found in response");
      }

      PositionGenerationResponse aiResponse =
          objectMapper.readValue(jsonResponse, PositionGenerationResponse.class);

      // Определяем уровень позиции
      Position.Level positionLevel = Position.Level.MIDDLE; // по умолчанию
      if (aiResponse.getLevel() != null) {
        String levelLower = aiResponse.getLevel().toLowerCase();
        if (levelLower.contains("junior")) {
          positionLevel = Position.Level.JUNIOR;
        } else if (levelLower.contains("senior")) {
          positionLevel = Position.Level.SENIOR;
        } else if (levelLower.contains("lead")) {
          positionLevel = Position.Level.LEAD;
        }
      }

      log.info(
          "Parsed position: title='{}', level='{}', topics={}, questions={}",
          aiResponse.getTitle(),
          positionLevel,
          aiResponse.getTopics(),
          aiResponse.getQuestions() != null ? aiResponse.getQuestions().size() : 0);

      return Position.builder()
          .title(aiResponse.getTitle().trim())
          .description(
              aiResponse.getDescription() != null && !aiResponse.getDescription().trim().isEmpty()
                  ? aiResponse.getDescription().trim()
                  : null)
          .status(Position.Status.ACTIVE)
          .level(positionLevel)
          .language(ApplicationProperties.Constants.DEFAULT_LANGUAGE)
          .showOtherLang(true) // дефолтное значение
          .answerTime(ApplicationProperties.Constants.DEFAULT_ANSWER_TIME)
          .saveAudio(true) // дефолтное значение
          .saveVideo(true) // дефолтное значение
          .randomOrder(true) // дефолтное значение
          .questionType(ApplicationProperties.Constants.DEFAULT_QUESTION_TYPE)
          .questionsCount(ApplicationProperties.Constants.DEFAULT_QUESTIONS_COUNT)
          .checkType(ApplicationProperties.Constants.DEFAULT_CHECK_TYPE)
          .minScore(ApplicationProperties.Constants.DEFAULT_MIN_SCORE)
          .topics(aiResponse.getTopics() != null ? aiResponse.getTopics() : new ArrayList<>())
          .build();

    } catch (Exception e) {
      log.warn("Failed to parse position response, creating default position", e);

      // Создаем минимальную вакансию
      return Position.builder()
          .title("Новая вакансия")
          .status(Position.Status.ACTIVE)
          .language("Русский")
          .showOtherLang(true)
          .answerTime(150)
          .saveAudio(true)
          .saveVideo(true)
          .randomOrder(true)
          .questionType("В основном хард-скиллы")
          .questionsCount(5)
          .checkType("Автоматическая проверка")
          .minScore(6.0)
          .level(Position.Level.MIDDLE)
          .topics(new ArrayList<>())
          .build();
    }
  }

  /** Парсит ответ AI в DTO PositionGenerationResponse. Используется для нового API endpoint. */
  private PositionGenerationResponse parsePositionGenerationResponse(String response) {
    try {
      String jsonResponse = extractJsonFromResponse(response);
      if (jsonResponse == null) {
        throw new RuntimeException("No JSON found in response");
      }

      PositionGenerationResponse aiResponse =
          objectMapper.readValue(jsonResponse, PositionGenerationResponse.class);

      log.info(
          "Parsed AI response: title='{}', level='{}', topics={}, questions={}",
          aiResponse.getTitle(),
          aiResponse.getLevel(),
          aiResponse.getTopics(),
          aiResponse.getQuestions() != null ? aiResponse.getQuestions().size() : 0);

      return aiResponse;

    } catch (Exception e) {
      log.warn("Failed to parse AI response, creating default response", e);

      // Создаем минимальный ответ
      PositionGenerationResponse defaultResponse = new PositionGenerationResponse();
      defaultResponse.setTitle("Новая вакансия");
      defaultResponse.setDescription("Описание вакансии");
      defaultResponse.setLevel("middle");
      defaultResponse.setTopics(new ArrayList<>());
      defaultResponse.setQuestions(new ArrayList<>());

      return defaultResponse;
    }
  }

  /** Извлекает JSON из текста ответа AI. */
  private String extractJsonFromResponse(String response) {
    try {
      // Ищем начало JSON объекта
      int startIndex = response.indexOf('{');
      if (startIndex == -1) {
        return null;
      }

      // Ищем конец JSON объекта
      int braceCount = 0;
      int endIndex = startIndex;

      for (int i = startIndex; i < response.length(); i++) {
        char c = response.charAt(i);
        if (c == '{') {
          braceCount++;
        } else if (c == '}') {
          braceCount--;
          if (braceCount == 0) {
            endIndex = i + 1;
            break;
          }
        }
      }

      if (braceCount == 0) {
        return response.substring(startIndex, endIndex);
      }

      return null;
    } catch (Exception e) {
      log.warn("Failed to extract JSON from response", e);
      return null;
    }
  }
}
