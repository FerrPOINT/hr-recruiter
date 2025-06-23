package azhukov.service.ai.claude;

import azhukov.config.ClaudeConfig;
import azhukov.service.ai.AIService;
import azhukov.service.ai.AIServiceException;
import azhukov.service.ai.AIUsageStats;
import azhukov.service.ai.claude.dto.ClaudeRequest;
import azhukov.service.ai.claude.dto.ClaudeResponse;
import azhukov.service.ai.claude.dto.ClaudeUsage;
import java.time.LocalDateTime;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Сервис для работы с Claude AI API. Следует принципам SOLID и DDD: - Single Responsibility:
 * отвечает только за взаимодействие с Claude API - Open/Closed: легко расширяется новыми методами
 * без изменения существующих - Dependency Inversion: зависит от абстракций (AIService interface)
 *
 * @author AI Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService implements AIService {

  // Конфигурация
  private final ClaudeConfig claudeConfig;
  private final RestTemplate restTemplate;

  // Статистика использования (Thread-safe)
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong successfulRequests = new AtomicLong(0);
  private final AtomicLong failedRequests = new AtomicLong(0);
  private final AtomicLong totalTokens = new AtomicLong(0);
  private final AtomicReference<LocalDateTime> firstRequestTime = new AtomicReference<>();
  private final AtomicReference<LocalDateTime> lastRequestTime = new AtomicReference<>();
  private final Map<String, Long> modelUsage = new ConcurrentHashMap<>();

  // Константы
  private static final String SERVICE_NAME = "Claude AI";
  private static final String DEFAULT_ROLE = "user";
  private static final String CONTENT_TYPE_TEXT = "text";

  /** {@inheritDoc} */
  @Override
  public String generateText(String prompt) throws AIServiceException {
    log.debug("Generating text with Claude, prompt length: {}", prompt.length());

    try {
      validateApiKey();
      updateRequestStats();

      ClaudeRequest request =
          ClaudeRequest.simpleRequest(claudeConfig.getModel(), prompt, claudeConfig.getMaxTokens());
      ClaudeResponse response = sendRequest(request);

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
    log.debug("Generating {} text items with Claude", count);

    try {
      validateApiKey();
      updateRequestStats();

      // Модифицируем промпт для генерации списка
      String listPrompt =
          String.format("%s\n\nВерни ровно %d элементов, каждый с новой строки:", prompt, count);

      ClaudeRequest request =
          ClaudeRequest.simpleRequest(
              claudeConfig.getModel(), listPrompt, claudeConfig.getMaxTokens());
      ClaudeResponse response = sendRequest(request);

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
      log.error("Claude service availability check failed", e);
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getModelInfo() {
    return String.format("Claude %s", claudeConfig.getModel());
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

  /**
   * Создает описание компании.
   *
   * @param companyInfo Информация о компании
   * @param targetAudience Целевая аудитория
   * @return Описание компании
   * @throws AIServiceException если произошла ошибка
   */
  public String createCompanyDescription(String companyInfo, String targetAudience)
      throws AIServiceException {
    log.info("Creating company description for target audience: {}", targetAudience);

    String prompt = buildCompanyDescriptionPrompt(companyInfo, targetAudience);
    return generateText(prompt);
  }

  /**
   * Улучшает email сообщение.
   *
   * @param originalEmail Исходный email
   * @param purpose Цель email
   * @param tone Тон сообщения
   * @return Улучшенный email
   * @throws AIServiceException если произошла ошибка
   */
  public String improveEmail(String originalEmail, String purpose, String tone)
      throws AIServiceException {
    log.info("Improving email with purpose: {} and tone: {}", purpose, tone);

    String prompt = buildEmailImprovementPrompt(originalEmail, purpose, tone);
    return generateText(prompt);
  }

  /**
   * Генерирует чек-лист для собеседования.
   *
   * @param positionTitle Название позиции
   * @param experienceLevel Уровень опыта
   * @return Чек-лист
   * @throws AIServiceException если произошла ошибка
   */
  public List<String> generateInterviewChecklist(String positionTitle, String experienceLevel)
      throws AIServiceException {
    log.info(
        "Generating interview checklist for position: {} with experience level: {}",
        positionTitle,
        experienceLevel);

    String prompt = buildInterviewChecklistPrompt(positionTitle, experienceLevel);
    return generateTextList(prompt, 10); // Стандартный чек-лист из 10 пунктов
  }

  /**
   * Анализирует резюме кандидата.
   *
   * @param resumeText Текст резюме
   * @param jobRequirements Требования к вакансии
   * @return Анализ резюме
   * @throws AIServiceException если произошла ошибка
   */
  public String analyzeResume(String resumeText, String jobRequirements) throws AIServiceException {
    log.info("Analyzing resume against job requirements");

    String prompt = buildResumeAnalysisPrompt(resumeText, jobRequirements);
    return generateText(prompt);
  }

  /**
   * Генерирует обратную связь для кандидата.
   *
   * @param candidateName Имя кандидата
   * @param interviewNotes Заметки по собеседованию
   * @param positionTitle Название позиции
   * @return Обратная связь
   * @throws AIServiceException если произошла ошибка
   */
  public String generateCandidateFeedback(
      String candidateName, String interviewNotes, String positionTitle) throws AIServiceException {

    log.info(
        "Generating feedback for candidate: {} for position: {}", candidateName, positionTitle);

    String prompt = buildCandidateFeedbackPrompt(candidateName, interviewNotes, positionTitle);
    return generateText(prompt);
  }

  // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

  /**
   * Отправляет запрос к Claude API.
   *
   * @param request Запрос
   * @return Ответ от API
   * @throws AIServiceException если произошла ошибка
   */
  private ClaudeResponse sendRequest(ClaudeRequest request) throws AIServiceException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<ClaudeRequest> entity = new HttpEntity<>(request, headers);

      log.debug("Sending request to Claude API: {}", request.getModel());

      ResponseEntity<ClaudeResponse> response =
          restTemplate.postForEntity(claudeConfig.getApiUrl(), entity, ClaudeResponse.class);

      if (response.getStatusCode() != HttpStatus.OK) {
        throw new AIServiceException(
            String.format("Claude API returned status: %s", response.getStatusCode()),
            AIServiceException.ErrorType.API_UNAVAILABLE,
            SERVICE_NAME);
      }

      return response.getBody();

    } catch (HttpClientErrorException e) {
      log.error("Claude API client error: {}", e.getMessage());
      throw new AIServiceException(
          "Claude API client error", AIServiceException.ErrorType.INVALID_REQUEST, SERVICE_NAME);
    } catch (HttpServerErrorException e) {
      log.error("Claude API server error: {}", e.getMessage());
      throw new AIServiceException(
          "Claude API server error", AIServiceException.ErrorType.API_UNAVAILABLE, SERVICE_NAME);
    } catch (Exception e) {
      log.error("Unexpected error during Claude API call", e);
      throw new AIServiceException(
          "Unexpected error during API call",
          AIServiceException.ErrorType.UNKNOWN_ERROR,
          SERVICE_NAME);
    }
  }

  /** Создает заголовки для API запроса. */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", claudeConfig.getApiKey());
    headers.set("anthropic-version", "2023-06-01");
    return headers;
  }

  /**
   * Извлекает содержимое из ответа Claude.
   *
   * @param response Ответ от API
   * @return Текстовое содержимое
   * @throws AIServiceException если содержимое недоступно
   */
  private String extractContent(ClaudeResponse response) throws AIServiceException {
    if (response == null || !response.isSuccessful()) {
      throw new AIServiceException(
          "Invalid response from Claude API",
          AIServiceException.ErrorType.API_UNAVAILABLE,
          SERVICE_NAME);
    }

    String content = response.getTextContent();
    if (content == null || content.trim().isEmpty()) {
      throw new AIServiceException(
          "Empty response from Claude API",
          AIServiceException.ErrorType.API_UNAVAILABLE,
          SERVICE_NAME);
    }

    return content.trim();
  }

  /**
   * Парсит ответ со списком элементов.
   *
   * @param content Содержимое ответа
   * @param expectedCount Ожидаемое количество элементов
   * @return Список элементов
   */
  private List<String> parseListResponse(String content, int expectedCount) {
    String[] lines = content.split("\n");
    return List.of(lines).stream()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .limit(expectedCount)
        .toList();
  }

  /**
   * Проверяет наличие API ключа.
   *
   * @throws AIServiceException если ключ отсутствует
   */
  private void validateApiKey() throws AIServiceException {
    if (claudeConfig.getApiKey() == null || claudeConfig.getApiKey().trim().isEmpty()) {
      throw new AIServiceException(
          "Claude API key is not configured",
          AIServiceException.ErrorType.API_KEY_MISSING,
          SERVICE_NAME);
    }
  }

  /** Обновляет статистику запросов. */
  private void updateRequestStats() {
    totalRequests.incrementAndGet();
    LocalDateTime now = LocalDateTime.now();
    lastRequestTime.set(now);
    if (firstRequestTime.get() == null) {
      firstRequestTime.set(now);
    }
  }

  /**
   * Обновляет статистику успешных запросов.
   *
   * @param usage Информация об использовании токенов
   */
  private void updateSuccessStats(ClaudeUsage usage) {
    successfulRequests.incrementAndGet();
    if (usage != null && usage.hasUsageInfo()) {
      totalTokens.addAndGet(usage.getTotalTokens());
    }
  }

  /** Обновляет статистику неудачных запросов. */
  private void updateFailureStats() {
    failedRequests.incrementAndGet();
  }

  /**
   * Создает исключение AIServiceException.
   *
   * @param message Сообщение об ошибке
   * @param cause Причина ошибки
   * @return Исключение
   */
  private AIServiceException createAIServiceException(String message, Throwable cause) {
    AIServiceException.ErrorType errorType = determineErrorType(cause);
    return new AIServiceException(message, cause, errorType, SERVICE_NAME);
  }

  /**
   * Определяет тип ошибки на основе причины.
   *
   * @param cause Причина ошибки
   * @return Тип ошибки
   */
  private AIServiceException.ErrorType determineErrorType(Throwable cause) {
    if (cause instanceof RestClientException) {
      return AIServiceException.ErrorType.NETWORK_ERROR;
    } else if (cause instanceof AIServiceException) {
      return ((AIServiceException) cause).getErrorType();
    } else {
      return AIServiceException.ErrorType.UNKNOWN_ERROR;
    }
  }

  /**
   * Вычисляет среднее время ответа.
   *
   * @return Среднее время ответа в миллисекундах
   */
  private double calculateAverageResponseTime() {
    long total = totalRequests.get();
    if (total == 0) {
      return 0.0;
    }
    // Упрощенный расчет - в реальном приложении нужно хранить время каждого запроса
    return 500.0; // Примерное среднее время
  }

  // ========== ПРОМПТЫ ==========

  /**
   * Строит промпт для генерации вопросов собеседования.
   *
   * @param jobDescription Описание вакансии
   * @param count Количество вопросов
   * @param questionTypes Типы вопросов
   * @return Промпт
   */
  private String buildInterviewQuestionsPrompt(
      String jobDescription, int count, List<String> questionTypes) {
    return String.format(
        """
            Сгенерируй %d вопросов для собеседования на основе следующего описания вакансии:

            %s

            Типы вопросов: %s

            Вопросы должны быть:
            - Релевантными для данной позиции
            - Разнообразными по сложности
            - Практическими и ситуационными
            - Направленными на оценку навыков и опыта

            Верни только вопросы, каждый с новой строки, без нумерации.
            """,
        count, jobDescription, String.join(", ", questionTypes));
  }

  /**
   * Строит промпт для анализа ответа кандидата.
   *
   * @param question Вопрос
   * @param candidateAnswer Ответ кандидата
   * @return Промпт
   */
  private String buildAnswerAnalysisPrompt(String question, String candidateAnswer) {
    return String.format(
        """
            Проанализируй ответ кандидата на следующий вопрос:

            Вопрос: %s
            Ответ кандидата: %s

            Дай оценку по следующим критериям:
            1. Релевантность ответа (0-10)
            2. Глубина знаний (0-10)
            3. Практический опыт (0-10)
            4. Коммуникативные навыки (0-10)
            5. Общая оценка (0-10)

            Также предоставь:
            - Краткий анализ сильных и слабых сторон
            - Рекомендации для дальнейшего собеседования
            - Общий вывод о кандидате
            """,
        question, candidateAnswer);
  }

  /**
   * Строит промпт для улучшения описания вакансии.
   *
   * @param originalDescription Исходное описание
   * @param improvementType Тип улучшения
   * @return Промпт
   */
  private String buildJobDescriptionImprovementPrompt(
      String originalDescription, String improvementType) {
    return String.format(
        """
            Улучши описание вакансии в соответствии с типом улучшения '%s':

            Исходное описание:
            %s

            Типы улучшений:
            - структура: улучшить структуру и логику изложения
            - детализация: добавить больше деталей и специфики
            - привлекательность: сделать описание более привлекательным для кандидатов

            Верни улучшенное описание, сохранив всю важную информацию.
            """,
        improvementType, originalDescription);
  }

  /**
   * Строит промпт для создания описания компании.
   *
   * @param companyInfo Информация о компании
   * @param targetAudience Целевая аудитория
   * @return Промпт
   */
  private String buildCompanyDescriptionPrompt(String companyInfo, String targetAudience) {
    return String.format(
        """
            Создай привлекательное описание компании на основе следующей информации:

            Информация о компании:
            %s

            Целевая аудитория: %s

            Описание должно быть:
            - Информативным и привлекательным
            - Адаптированным под целевую аудиторию
            - Подчеркивающим уникальные преимущества компании
            - Мотивирующим к сотрудничеству

            Верни готовое описание компании.
            """,
        companyInfo, targetAudience);
  }

  /**
   * Строит промпт для улучшения email.
   *
   * @param originalEmail Исходный email
   * @param purpose Цель email
   * @param tone Тон сообщения
   * @return Промпт
   */
  private String buildEmailImprovementPrompt(String originalEmail, String purpose, String tone) {
    return String.format(
        """
            Улучши следующее email сообщение:

            Исходный email:
            %s

            Цель email: %s
            Желаемый тон: %s

            Улучшения должны включать:
            - Более четкую структуру
            - Улучшенную грамматику и стиль
            - Соответствие цели и тону
            - Более эффективное воздействие на получателя

            Верни улучшенную версию email.
            """,
        originalEmail, purpose, tone);
  }

  /**
   * Строит промпт для генерации чек-листа собеседования.
   *
   * @param positionTitle Название позиции
   * @param experienceLevel Уровень опыта
   * @return Промпт
   */
  private String buildInterviewChecklistPrompt(String positionTitle, String experienceLevel) {
    return String.format(
        """
            Создай чек-лист для проведения собеседования на позицию '%s' с уровнем опыта '%s'.

            Чек-лист должен включать:
            - Подготовительные вопросы
            - Технические аспекты для проверки
            - Поведенческие вопросы
            - Критерии оценки
            - Заключительные шаги

            Верни список пунктов чек-листа, каждый с новой строки.
            """,
        positionTitle, experienceLevel);
  }

  /**
   * Строит промпт для анализа резюме.
   *
   * @param resumeText Текст резюме
   * @param jobRequirements Требования к вакансии
   * @return Промпт
   */
  private String buildResumeAnalysisPrompt(String resumeText, String jobRequirements) {
    return String.format(
        """
            Проанализируй резюме кандидата в контексте требований к вакансии:

            Резюме кандидата:
            %s

            Требования к вакансии:
            %s

            Дай анализ по следующим пунктам:
            1. Соответствие требованиям (0-10)
            2. Опыт работы (0-10)
            3. Навыки и компетенции (0-10)
            4. Образование и сертификации (0-10)
            5. Общая оценка (0-10)

            Также укажи:
            - Сильные стороны кандидата
            - Слабые стороны или недостатки
            - Рекомендации по дальнейшему рассмотрению
            - Общий вывод о пригодности кандидата
            """,
        resumeText, jobRequirements);
  }

  /**
   * Строит промпт для генерации обратной связи кандидату.
   *
   * @param candidateName Имя кандидата
   * @param interviewNotes Заметки по собеседованию
   * @param positionTitle Название позиции
   * @return Промпт
   */
  private String buildCandidateFeedbackPrompt(
      String candidateName, String interviewNotes, String positionTitle) {
    return String.format(
        """
            Создай профессиональную обратную связь для кандидата на основе результатов собеседования:

            Имя кандидата: %s
            Позиция: %s
            Заметки по собеседованию:
            %s

            Обратная связь должна включать:
            - Общую оценку выступления
            - Конкретные сильные стороны
            - Области для улучшения
            - Конструктивные рекомендации
            - Профессиональный и вежливый тон

            Верни готовую обратную связь для отправки кандидату.
            """,
        candidateName, positionTitle, interviewNotes);
  }
}
