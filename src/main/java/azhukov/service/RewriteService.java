package azhukov.service;

import azhukov.service.ai.AIService;
import azhukov.service.ai.AIServiceException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для переписывания и улучшения текстов с использованием AI. Следует принципам SOLID и DDD:
 * - Single Responsibility: отвечает только за переписывание текстов - Open/Closed: легко
 * расширяется новыми типами переписывания - Dependency Inversion: зависит от абстракции AIService -
 * Strategy Pattern: использует разные стратегии для разных типов текстов
 *
 * @author AI Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewriteService {

  // Зависимости
  private final AIService aiService;

  // Кэш для часто используемых промптов (Strategy Pattern)
  private final Map<String, String> promptCache = new ConcurrentHashMap<>();

  // Константы для типов переписывания
  public static final String TYPE_PROFESSIONAL = "professional";
  public static final String TYPE_CASUAL = "casual";
  public static final String TYPE_FORMAL = "formal";
  public static final String TYPE_CONCISE = "concise";
  public static final String TYPE_DETAILED = "detailed";
  public static final String TYPE_PERSUASIVE = "persuasive";
  public static final String TYPE_EDUCATIONAL = "educational";
  public static final String TYPE_CREATIVE = "creative";

  // Константы для целевых аудиторий
  public static final String AUDIENCE_EXECUTIVES = "executives";
  public static final String AUDIENCE_DEVELOPERS = "developers";
  public static final String AUDIENCE_HR = "hr";
  public static final String AUDIENCE_CANDIDATES = "candidates";
  public static final String AUDIENCE_CLIENTS = "clients";

  /**
   * Переписывает текст в профессиональном стиле.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteProfessional(String originalText) throws AIServiceException {
    log.info("Rewriting text in professional style, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_PROFESSIONAL, AUDIENCE_EXECUTIVES);
  }

  /**
   * Переписывает текст в неформальном стиле.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteCasual(String originalText) throws AIServiceException {
    log.info("Rewriting text in casual style, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_CASUAL, AUDIENCE_CANDIDATES);
  }

  /**
   * Переписывает текст в формальном стиле.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteFormal(String originalText) throws AIServiceException {
    log.info("Rewriting text in formal style, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_FORMAL, AUDIENCE_EXECUTIVES);
  }

  /**
   * Переписывает текст, делая его более кратким.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteConcise(String originalText) throws AIServiceException {
    log.info("Rewriting text to be more concise, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_CONCISE, AUDIENCE_EXECUTIVES);
  }

  /**
   * Переписывает текст, добавляя больше деталей.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteDetailed(String originalText) throws AIServiceException {
    log.info("Rewriting text with more details, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_DETAILED, AUDIENCE_DEVELOPERS);
  }

  /**
   * Переписывает текст, делая его более убедительным.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewritePersuasive(String originalText) throws AIServiceException {
    log.info("Rewriting text to be more persuasive, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_PERSUASIVE, AUDIENCE_CLIENTS);
  }

  /**
   * Переписывает текст в образовательном стиле.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteEducational(String originalText) throws AIServiceException {
    log.info("Rewriting text in educational style, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_EDUCATIONAL, AUDIENCE_DEVELOPERS);
  }

  /**
   * Переписывает текст в креативном стиле.
   *
   * @param originalText Исходный текст
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteCreative(String originalText) throws AIServiceException {
    log.info("Rewriting text in creative style, length: {}", originalText.length());
    return rewriteText(originalText, TYPE_CREATIVE, AUDIENCE_CANDIDATES);
  }

  /**
   * Переписывает текст с кастомными параметрами.
   *
   * @param originalText Исходный текст
   * @param style Стиль переписывания
   * @param targetAudience Целевая аудитория
   * @return Переписанный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String rewriteText(String originalText, String style, String targetAudience)
      throws AIServiceException {
    log.debug("Rewriting text with style: {}, audience: {}", style, targetAudience);

    try {
      String prompt = buildRewritePrompt(originalText, style, targetAudience);
      return aiService.generateText(prompt);
    } catch (AIServiceException e) {
      log.error("Failed to rewrite text with AI service", e);
      throw e;
    }
  }

  /**
   * Переписывает несколько вариантов текста.
   *
   * @param originalText Исходный текст
   * @param styles Список стилей для переписывания
   * @return Map с вариантами переписанного текста
   * @throws AIServiceException если произошла ошибка
   */
  public Map<String, String> rewriteMultipleVersions(String originalText, List<String> styles)
      throws AIServiceException {
    log.info("Rewriting text in {} different styles", styles.size());

    Map<String, String> results = new ConcurrentHashMap<>();

    for (String style : styles) {
      try {
        String rewritten = rewriteText(originalText, style, getDefaultAudienceForStyle(style));
        results.put(style, rewritten);
      } catch (AIServiceException e) {
        log.error("Failed to rewrite text in style: {}", style, e);
        results.put(style, "Ошибка при переписывании: " + e.getMessage());
      }
    }

    return results;
  }

  /**
   * Улучшает описание вакансии.
   *
   * @param jobDescription Исходное описание вакансии
   * @param positionTitle Название позиции
   * @param companyName Название компании
   * @return Улучшенное описание
   * @throws AIServiceException если произошла ошибка
   */
  public String improveJobDescription(
      String jobDescription, String positionTitle, String companyName) throws AIServiceException {

    log.info(
        "Improving job description for position: {} at company: {}", positionTitle, companyName);

    String prompt =
        buildJobDescriptionImprovementPrompt(jobDescription, positionTitle, companyName);
    return aiService.generateText(prompt);
  }

  /**
   * Улучшает email-сообщение.
   *
   * @param email Исходный email
   * @param purpose Назначение email
   * @param recipientType Тип получателя
   * @return Улучшенный email
   * @throws AIServiceException если произошла ошибка
   */
  public String improveEmail(String email, String purpose, String recipientType)
      throws AIServiceException {
    log.info("Improving email for purpose: {} to recipient type: {}", purpose, recipientType);

    String prompt = buildEmailImprovementPrompt(email, purpose, recipientType);
    return aiService.generateText(prompt);
  }

  /**
   * Улучшает описание компании.
   *
   * @param companyDescription Исходное описание компании
   * @param industry Отрасль
   * @param targetAudience Целевая аудитория
   * @return Улучшенное описание
   * @throws AIServiceException если произошла ошибка
   */
  public String improveCompanyDescription(
      String companyDescription, String industry, String targetAudience) throws AIServiceException {

    log.info(
        "Improving company description for industry: {} and audience: {}",
        industry,
        targetAudience);

    String prompt =
        buildCompanyDescriptionImprovementPrompt(companyDescription, industry, targetAudience);
    return aiService.generateText(prompt);
  }

  /**
   * Улучшает ответы на вопросы интервью.
   *
   * @param question Вопрос
   * @param originalAnswer Исходный ответ
   * @param positionTitle Название позиции
   * @return Улучшенный ответ
   * @throws AIServiceException если произошла ошибка
   */
  public String improveInterviewAnswer(String question, String originalAnswer, String positionTitle)
      throws AIServiceException {

    log.info("Improving interview answer for position: {}", positionTitle);

    String prompt = buildInterviewAnswerImprovementPrompt(question, originalAnswer, positionTitle);
    return aiService.generateText(prompt);
  }

  /**
   * Генерирует альтернативные формулировки.
   *
   * @param text Исходный текст
   * @param count Количество альтернатив
   * @return Список альтернативных формулировок
   * @throws AIServiceException если произошла ошибка
   */
  public List<String> generateAlternatives(String text, int count) throws AIServiceException {
    log.info("Generating {} alternative formulations", count);

    String prompt = buildAlternativesPrompt(text, count);
    return aiService.generateTextList(prompt, count);
  }

  /**
   * Проверяет и исправляет грамматику и стиль.
   *
   * @param text Исходный текст
   * @return Исправленный текст
   * @throws AIServiceException если произошла ошибка
   */
  public String checkAndFixGrammar(String text) throws AIServiceException {
    log.info("Checking and fixing grammar for text");

    String prompt = buildGrammarCheckPrompt(text);
    return aiService.generateText(prompt);
  }

  // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

  /**
   * Строит промпт для переписывания текста.
   *
   * @param originalText Исходный текст
   * @param style Стиль переписывания
   * @param targetAudience Целевая аудитория
   * @return Промпт для AI
   */
  private String buildRewritePrompt(String originalText, String style, String targetAudience) {
    String cacheKey = String.format("%s_%s_%s", style, targetAudience, originalText.length());

    return promptCache.computeIfAbsent(
        cacheKey,
        k -> {
          String styleInstructions = getStyleInstructions(style);
          String audienceInstructions = getAudienceInstructions(targetAudience);

          return String.format(
              """
                Перепиши следующий текст в стиле "%s" для аудитории "%s".

                %s
                %s

                Исходный текст:
                %s

                Требования:
                - Сохрани основную идею и ключевую информацию
                - Используй указанный стиль и тон
                - Адаптируй под целевую аудиторию
                - Сделай текст более читаемым и понятным
                - Сохрани профессиональный уровень

                Верни только переписанный текст без дополнительных комментариев.
                """,
              style, targetAudience, styleInstructions, audienceInstructions, originalText);
        });
  }

  /**
   * Получает инструкции для стиля.
   *
   * @param style Стиль
   * @return Инструкции
   */
  private String getStyleInstructions(String style) {
    return switch (style) {
      case TYPE_PROFESSIONAL ->
          "Используй профессиональный, деловой стиль с четкой структурой и формальным тоном.";
      case TYPE_CASUAL -> "Используй неформальный, дружелюбный стиль с простыми предложениями.";
      case TYPE_FORMAL ->
          "Используй очень формальный стиль с официальной лексикой и сложными конструкциями.";
      case TYPE_CONCISE -> "Сделай текст максимально кратким, убрав лишние слова и детали.";
      case TYPE_DETAILED -> "Добавь больше деталей, примеров и объяснений.";
      case TYPE_PERSUASIVE ->
          "Сделай текст более убедительным с использованием аргументов и призывов к действию.";
      case TYPE_EDUCATIONAL -> "Используй образовательный стиль с объяснениями и примерами.";
      case TYPE_CREATIVE ->
          "Используй креативный стиль с образными выражениями и нестандартными формулировками.";
      default -> "Используй нейтральный стиль с балансом между формальностью и доступностью.";
    };
  }

  /**
   * Получает инструкции для аудитории.
   *
   * @param audience Аудитория
   * @return Инструкции
   */
  private String getAudienceInstructions(String audience) {
    return switch (audience) {
      case AUDIENCE_EXECUTIVES ->
          "Адаптируй для руководителей высшего звена с акцентом на стратегические аспекты.";
      case AUDIENCE_DEVELOPERS ->
          "Адаптируй для технических специалистов с использованием соответствующей терминологии.";
      case AUDIENCE_HR -> "Адаптируй для HR-специалистов с акцентом на процессы и процедуры.";
      case AUDIENCE_CANDIDATES ->
          "Адаптируй для кандидатов с понятным и привлекательным изложением.";
      case AUDIENCE_CLIENTS -> "Адаптируй для клиентов с акцентом на ценность и преимущества.";
      default -> "Используй универсальный подход для широкой аудитории.";
    };
  }

  /**
   * Получает аудиторию по умолчанию для стиля.
   *
   * @param style Стиль
   * @return Аудитория по умолчанию
   */
  private String getDefaultAudienceForStyle(String style) {
    return switch (style) {
      case TYPE_PROFESSIONAL, TYPE_FORMAL -> AUDIENCE_EXECUTIVES;
      case TYPE_DETAILED, TYPE_EDUCATIONAL -> AUDIENCE_DEVELOPERS;
      case TYPE_CASUAL, TYPE_CREATIVE -> AUDIENCE_CANDIDATES;
      case TYPE_PERSUASIVE -> AUDIENCE_CLIENTS;
      default -> AUDIENCE_HR;
    };
  }

  /** Строит промпт для улучшения описания вакансии. */
  private String buildJobDescriptionImprovementPrompt(
      String jobDescription, String positionTitle, String companyName) {
    return String.format(
        """
            Улучши описание вакансии, сделав его более привлекательным и профессиональным.

            Позиция: %s
            Компания: %s

            Исходное описание:
            %s

            Требования к улучшению:
            - Сделай описание более структурированным и читаемым
            - Добавь привлекательные элементы и преимущества компании
            - Используй профессиональный, но дружелюбный тон
            - Подчеркни возможности для роста и развития
            - Сделай требования более четкими и понятными
            - Добавь информацию о корпоративной культуре
            - Оптимальная длина: 300-500 слов

            Верни только улучшенное описание без дополнительных комментариев.
            """,
        positionTitle, companyName, jobDescription);
  }

  /** Строит промпт для улучшения email. */
  private String buildEmailImprovementPrompt(String email, String purpose, String recipientType) {
    return String.format(
        """
            Улучши email-сообщение, сделав его более эффективным и профессиональным.

            Назначение: %s
            Тип получателя: %s

            Исходный email:
            %s

            Требования к улучшению:
            - Профессиональный и вежливый тон
            - Четкая структура с приветствием, основной частью и заключением
            - Конкретный призыв к действию
            - Корректная грамматика и пунктуация
            - Соответствие назначению и типу получателя
            - Оптимальная длина: 100-200 слов

            Верни только улучшенный email без дополнительных комментариев.
            """,
        purpose, recipientType, email);
  }

  /** Строит промпт для улучшения описания компании. */
  private String buildCompanyDescriptionImprovementPrompt(
      String companyDescription, String industry, String targetAudience) {
    return String.format(
        """
            Улучши описание компании, сделав его более привлекательным и информативным.

            Отрасль: %s
            Целевая аудитория: %s

            Исходное описание:
            %s

            Требования к улучшению:
            - Профессиональный и привлекательный тон
            - Структурированное описание с ключевыми преимуществами
            - Информация о миссии, ценностях и достижениях
            - Адаптация под целевую аудиторию
            - Подчеркивание уникальности компании
            - Оптимальная длина: 200-400 слов

            Верни только улучшенное описание без дополнительных комментариев.
            """,
        industry, targetAudience, companyDescription);
  }

  /** Строит промпт для улучшения ответа на интервью. */
  private String buildInterviewAnswerImprovementPrompt(
      String question, String originalAnswer, String positionTitle) {
    return String.format(
        """
            Улучши ответ кандидата на вопрос интервью, сделав его более профессиональным и убедительным.

            Позиция: %s
            Вопрос: %s

            Исходный ответ:
            %s

            Требования к улучшению:
            - Структурированный ответ с введением, основной частью и заключением
            - Конкретные примеры и достижения
            - Профессиональный тон и лексика
            - Соответствие требованиям позиции
            - Убедительность и уверенность
            - Оптимальная длина: 150-300 слов

            Верни только улучшенный ответ без дополнительных комментариев.
            """,
        positionTitle, question, originalAnswer);
  }

  /** Строит промпт для генерации альтернатив. */
  private String buildAlternativesPrompt(String text, int count) {
    return String.format(
        """
            Создай %d альтернативных формулировок для следующего текста.

            Исходный текст:
            %s

            Требования к альтернативам:
            - Сохрани основную идею и смысл
            - Используй разные стили и подходы
            - Каждая альтернатива должна быть уникальной
            - Профессиональный уровень
            - Разная длина и структура

            Верни только альтернативы, каждую с новой строки, без нумерации.
            """,
        count, text);
  }

  /** Строит промпт для проверки грамматики. */
  private String buildGrammarCheckPrompt(String text) {
    return String.format(
        """
            Проверь и исправь грамматику, пунктуацию и стиль следующего текста.

            Исходный текст:
            %s

            Требования к исправлению:
            - Исправь все грамматические ошибки
            - Проверь пунктуацию
            - Улучши стиль и читаемость
            - Сохрани оригинальный смысл и тон
            - Сделай текст более профессиональным

            Верни только исправленный текст без дополнительных комментариев.
            """,
        text);
  }
}
