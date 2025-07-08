package azhukov.service;

import azhukov.config.ApplicationProperties;
import azhukov.entity.Interview;
import azhukov.entity.InterviewAnswer;
import azhukov.entity.Position;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.service.ai.AIService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для фоновой оценки завершенных собеседований через AI */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewEvaluationService {

  private final InterviewRepository interviewRepository;
  private final InterviewAnswerRepository interviewAnswerRepository;
  private final AIService aiService;

  /** Фоновая задача - оценивает завершенные собеседования каждую 1 минуту */
  @Scheduled(fixedRate = 60000) // 1 минута
  @Transactional
  public void evaluateFinishedInterviews() {
    try {
      List<Interview> finishedInterviews =
          interviewRepository.findByStatusAndResultIsNullWithAnswers(Interview.Status.FINISHED);

      // Логируем только если есть интервью для оценки
      if (!finishedInterviews.isEmpty()) {
        log.info(
            "Starting background evaluation of {} finished interviews", finishedInterviews.size());

        for (Interview interview : finishedInterviews) {
          try {
            evaluateInterview(interview);
          } catch (Exception e) {
            log.error("Error evaluating interview {}: {}", interview.getId(), e.getMessage(), e);

            // Если не удалось оценить, устанавливаем результат ERROR
            try {
              interview.setResult(Interview.Result.ERROR);
              interview.setAiScore(0.0);
              interviewRepository.save(interview);
              log.info(
                  "Set ERROR result for interview {} due to evaluation failure", interview.getId());
            } catch (Exception saveError) {
              log.error(
                  "Failed to set ERROR result for interview {}: {}",
                  interview.getId(),
                  saveError.getMessage());
            }
          }
        }

        log.info("Background evaluation completed for {} interviews", finishedInterviews.size());
      }
      // Если нет интервью для оценки - не логируем ничего
    } catch (Exception e) {
      log.error("Error in background evaluation task", e);
    }
  }

  /** Оценивает конкретное собеседование */
  @Transactional
  public void evaluateInterview(Interview interview) {
    List<InterviewAnswer> answers = interview.getAnswers();

    log.info(
        "Evaluating interview {} ({} answers, position: {}, level: {})",
        interview.getId(),
        answers.size(),
        interview.getPosition().getTitle(),
        interview.getPosition().getLevel());

    if (answers.isEmpty()) {
      log.warn(
          "Interview {} has no answers to evaluate, setting result to ERROR", interview.getId());

      // Устанавливаем результат как ошибку, если нет ответов
      interview.setAiScore(0.0);
      interview.setResult(Interview.Result.ERROR);
      interviewRepository.save(interview);
      return;
    }

    Position position = interview.getPosition();
    List<Double> scores = new ArrayList<>();

    // Оцениваем каждый ответ
    for (InterviewAnswer answer : answers) {
      double score = evaluateAnswer(answer, position);
      if (score > 0) {
        scores.add(score);
        log.debug(
            "Answer {} scored: {} (question: {})",
            answer.getId(),
            score,
            answer
                    .getQuestion()
                    .getText()
                    .substring(0, Math.min(50, answer.getQuestion().getText().length()))
                + "...");
      }
    }

    if (!scores.isEmpty()) {
      // Вычисляем средний балл
      double averageScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      // Округляем до десятых вверх
      averageScore = Math.ceil(averageScore * 10) / 10.0;

      // Сохраняем средний балл в интервью
      interview.setAiScore(averageScore);

      // Определяем результат
      Interview.Result result = determineResult(averageScore, position.getMinScore());
      interview.setResult(result);

      // Сохраняем результат в базу данных
      interviewRepository.save(interview);

      log.info(
          "Interview {} successfully evaluated: average score = {}/10, result = {}, min required = {}",
          interview.getId(),
          averageScore,
          result,
          position.getMinScore() != null ? position.getMinScore() : "default");
    } else {
      log.warn(
          "No answers were successfully evaluated for interview {}, setting result to ERROR",
          interview.getId());

      // Устанавливаем результат как ошибку, если нет оценок
      interview.setAiScore(0.0);
      interview.setResult(Interview.Result.ERROR);
      interviewRepository.save(interview);
    }
  }

  /** Оценивает конкретный ответ через AI */
  public double evaluateAnswer(InterviewAnswer answer, Position position) {
    String answerText =
        answer.getFormattedTranscription() != null
            ? answer.getFormattedTranscription()
            : answer.getAnswerText();

    if (answerText == null || answerText.trim().isEmpty()) {
      log.warn(
          "Answer {} has no text to evaluate (question: {})",
          answer.getId(),
          answer
                  .getQuestion()
                  .getText()
                  .substring(0, Math.min(30, answer.getQuestion().getText().length()))
              + "...");
      return 0.0;
    }

    try {
      log.debug(
          "Evaluating answer {} (question: {}, answer length: {} chars)",
          answer.getId(),
          answer
                  .getQuestion()
                  .getText()
                  .substring(0, Math.min(30, answer.getQuestion().getText().length()))
              + "...",
          answerText.length());

      // Создаем промпт для оценки
      String evaluationPrompt = buildEvaluationPrompt(answerText, position, answer.getQuestion());
      String scoreResponse = aiService.generateText(evaluationPrompt);
      double score = parseScore(scoreResponse);

      // Создаем промпт для детального обоснования
      String feedbackPrompt =
          buildFeedbackPrompt(answerText, position, answer.getQuestion(), score);
      String detailedFeedback = aiService.generateText(feedbackPrompt);

      // Сохраняем оценку и детальное обоснование
      answer.setScore(score);
      answer.setFeedback(detailedFeedback);
      answer.setScoreJustification(scoreResponse); // Сохраняем исходный ответ с оценкой

      // Сохраняем в базу данных
      interviewAnswerRepository.save(answer);

      log.info(
          "Answer {} successfully evaluated: score = {}/10 (question: {})",
          answer.getId(),
          score,
          answer
                  .getQuestion()
                  .getText()
                  .substring(0, Math.min(40, answer.getQuestion().getText().length()))
              + "...");
      return score;
    } catch (Exception e) {
      log.error(
          "Error evaluating answer {} (question: {}): {}",
          answer.getId(),
          answer
                  .getQuestion()
                  .getText()
                  .substring(0, Math.min(30, answer.getQuestion().getText().length()))
              + "...",
          e.getMessage());

      // Устанавливаем оценку 0 и сохраняем информацию об ошибке
      answer.setScore(0.0);
      answer.setFeedback("Ошибка оценки: " + e.getMessage());
      answer.setScoreJustification("Не удалось получить оценку из-за технической ошибки");
      interviewAnswerRepository.save(answer);

      return 0.0;
    }
  }

  /** Строит промпт для оценки ответа */
  private String buildEvaluationPrompt(
      String answerText, Position position, azhukov.entity.Question question) {

    // Определяем критерии оценки в зависимости от уровня позиции
    String levelCriteria = getLevelSpecificCriteria(position.getLevel());
    String positionContext = getPositionContext(position);
    String questionTypeCriteria = getQuestionTypeCriteria(question.getType());

    return String.format(
        """
        Ты - опытный HR-специалист и технический рекрутер. Оцени ответ кандидата на вопрос собеседования.

        === КОНТЕКСТ ПОЗИЦИИ ===
        Позиция: %s
        Уровень: %s
        Компания: %s
        Тип вопроса: %s
        %s

        === ВОПРОС ===
        %s

        === ОТВЕТ КАНДИДАТА ===
        %s

        === КРИТЕРИИ ОЦЕНКИ ===
        %s

        === ДОПОЛНИТЕЛЬНЫЕ КРИТЕРИИ ПО ТИПУ ВОПРОСА ===
        %s

        === ШКАЛА ОЦЕНКИ ===
        10 - Отличный ответ: полный, структурированный, с конкретными примерами, демонстрирует глубокие знания и опыт
        9 - Очень хороший ответ: подробный, с примерами, показывает хорошее понимание темы
        8 - Хороший ответ: полный ответ с некоторыми примерами, демонстрирует понимание
        7 - Удовлетворительный ответ: в целом правильный, но поверхностный или без примеров
        6 - Приемлемый ответ: основная идея понятна, но ответ неполный или неточный
        5 - Слабый ответ: частично правильный, но с существенными пробелами
        4 - Неудовлетворительный ответ: поверхностный, без понимания сути
        3 - Плохой ответ: неверный или очень неполный
        2 - Очень плохой ответ: практически не по теме
        1 - Неприемлемый ответ: полное непонимание вопроса

        === ИНСТРУКЦИИ ===
        - Оценивай ТОЛЬКО качество ответа, не личность кандидата
        - Учитывай уровень позиции при оценке
        - Учитывай тип вопроса при оценке
        - Если ответ слишком короткий или неясный - снижай оценку
        - Если есть конкретные примеры и детали - повышай оценку
        - Если ответ показывает практический опыт - это плюс
        - Если ответ только теоретический без примеров - снижай оценку

        === ФОРМАТ ОТВЕТА ===
        Верни ТОЛЬКО число от 1 до 10 без дополнительного текста, комментариев или объяснений.
        """,
        position.getTitle(),
        position.getLevel(),
        position.getCompany() != null ? position.getCompany() : "Не указана",
        question.getType(),
        positionContext,
        question.getText(),
        answerText,
        levelCriteria,
        questionTypeCriteria);
  }

  /** Получает критерии оценки в зависимости от уровня позиции */
  private String getLevelSpecificCriteria(Position.Level level) {
    return switch (level) {
      case JUNIOR ->
          """
          КРИТЕРИИ ДЛЯ JUNIOR ПОЗИЦИИ:
          - Базовые знания по теме (40%%)
          - Стремление к обучению и развитию (30%%)
          - Способность объяснить простые концепции (20%%)
          - Энтузиазм и мотивация (10%%)

          ОЖИДАНИЯ:
          - Не требуется глубокий опыт
          - Важны базовые знания и готовность учиться
          - Простые примеры из учебы или небольших проектов
          - Искренность и желание развиваться""";

      case MIDDLE ->
          """
          КРИТЕРИИ ДЛЯ MIDDLE ПОЗИЦИИ:
          - Практический опыт и знания (40%%)
          - Способность решать типовые задачи (30%%)
          - Умение объяснить сложные концепции (20%%)
          - Опыт работы в команде (10%%)

          ОЖИДАНИЯ:
          - Конкретные примеры из работы
          - Понимание практических аспектов
          - Опыт решения реальных задач
          - Умение работать в команде""";

      case SENIOR ->
          """
          КРИТЕРИИ ДЛЯ SENIOR ПОЗИЦИИ:
          - Глубокие технические знания (35%%)
          - Архитектурное мышление (25%%)
          - Опыт руководства и менторства (20%%)
          - Стратегическое мышление (20%%)

          ОЖИДАНИЯ:
          - Сложные технические примеры
          - Опыт принятия архитектурных решений
          - Примеры менторства и руководства
          - Понимание бизнес-контекста""";

      case LEAD ->
          """
          КРИТЕРИИ ДЛЯ LEAD ПОЗИЦИИ:
          - Лидерские качества (40%%)
          - Стратегическое видение (30%%)
          - Опыт управления командой (20%%)
          - Техническая экспертиза (10%%)

          ОЖИДАНИЯ:
          - Примеры лидерства и управления
          - Стратегическое мышление
          - Опыт масштабирования команд
          - Понимание бизнес-стратегии""";

      default ->
          """
          ОБЩИЕ КРИТЕРИИ:
          - Соответствие ответа вопросу (40%%)
          - Глубина знаний (30%%)
          - Практический опыт (20%%)
          - Качество изложения (10%%)""";
    };
  }

  /** Получает контекст позиции для оценки */
  private String getPositionContext(Position position) {
    StringBuilder context = new StringBuilder();

    if (position.getDescription() != null && !position.getDescription().trim().isEmpty()) {
      context.append("Описание позиции: ").append(position.getDescription()).append("\n");
    }

    if (position.getTopics() != null && !position.getTopics().isEmpty()) {
      context
          .append("Ключевые темы: ")
          .append(String.join(", ", position.getTopics()))
          .append("\n");
    }

    if (position.getQuestionType() != null && !position.getQuestionType().trim().isEmpty()) {
      context.append("Тип вопросов: ").append(position.getQuestionType()).append("\n");
    }

    return context.toString();
  }

  /** Строит промпт для детального обоснования оценки */
  private String buildFeedbackPrompt(
      String answerText, Position position, azhukov.entity.Question question, double score) {

    String scoreDescription = getScoreDescription(score);
    String improvementSuggestions = getImprovementSuggestions(score, position.getLevel());

    return String.format(
        """
        Ты - опытный HR-специалист. Дай детальную обратную связь по ответу кандидата.

        === КОНТЕКСТ ===
        Позиция: %s
        Уровень: %s
        Вопрос: %s
        Ответ кандидата: %s
        Оценка: %s/10 (%s)

        === ЗАДАЧА ===
        Предоставь структурированную обратную связь, которая включает:

        1. СИЛЬНЫЕ СТОРОНЫ (что кандидат сделал хорошо)
        2. ОБЛАСТИ ДЛЯ УЛУЧШЕНИЯ (что можно улучшить)
        3. КОНКРЕТНЫЕ РЕКОМЕНДАЦИИ (как улучшить ответ)
        4. ОБЩАЯ ОЦЕНКА (краткое резюме)

        === ИНСТРУКЦИИ ===
        - Будь конструктивным и профессиональным
        - Давай конкретные, а не общие рекомендации
        - Учитывай уровень позиции при рекомендациях
        - Если оценка низкая, объясни почему и как исправить
        - Если оценка высокая, подчеркни сильные стороны
        - Используй профессиональный, но дружелюбный тон

        === РЕКОМЕНДАЦИИ ПО УЛУЧШЕНИЮ ===
        %s

        === ФОРМАТ ОТВЕТА ===
        Верни структурированную обратную связь в следующем формате:

        СИЛЬНЫЕ СТОРОНЫ:
        [перечисли сильные стороны ответа]

        ОБЛАСТИ ДЛЯ УЛУЧШЕНИЯ:
        [перечисли что можно улучшить]

        РЕКОМЕНДАЦИИ:
        [конкретные советы по улучшению]

        ОБЩАЯ ОЦЕНКА:
        [краткое резюме оценки]
        """,
        position.getTitle(),
        position.getLevel(),
        question.getText(),
        answerText,
        score,
        scoreDescription,
        improvementSuggestions);
  }

  /** Получает описание оценки */
  private String getScoreDescription(double score) {
    if (score >= 9) return "Отлично";
    if (score >= 8) return "Очень хорошо";
    if (score >= 7) return "Хорошо";
    if (score >= 6) return "Удовлетворительно";
    if (score >= 5) return "Приемлемо";
    if (score >= 4) return "Слабо";
    if (score >= 3) return "Плохо";
    if (score >= 2) return "Очень плохо";
    return "Неприемлемо";
  }

  /** Получает рекомендации по улучшению в зависимости от оценки и уровня */
  private String getImprovementSuggestions(double score, Position.Level level) {
    if (score >= 8) {
      return """
          Для высоких оценок (8-10):
          - Подчеркни сильные стороны
          - Предложи дальнейшее развитие
          - Отметь профессиональный рост""";
    } else if (score >= 6) {
      return """
          Для средних оценок (6-7):
          - Предложи конкретные примеры
          - Рекомендуй углубление знаний
          - Посоветуй практические проекты""";
    } else {
      return switch (level) {
        case JUNIOR ->
            """
            Для низких оценок Junior позиции:
            - Рекомендуй базовое обучение
            - Предложи простые проекты для практики
            - Посоветуй изучение основ""";
        case MIDDLE ->
            """
            Для низких оценок Middle позиции:
            - Рекомендуй углубление практического опыта
            - Предложи решение реальных задач
            - Посоветуй менторство""";
        case SENIOR ->
            """
            Для низких оценок Senior позиции:
            - Рекомендуй развитие архитектурного мышления
            - Предложи опыт руководства
            - Посоветуй стратегическое планирование""";
        case LEAD ->
            """
            Для низких оценок Lead позиции:
            - Рекомендуй развитие лидерских качеств
            - Предложи опыт управления командой
            - Посоветуй стратегическое видение""";
        default ->
            """
            Для низких оценок:
            - Рекомендуй изучение основ
            - Предложи практические проекты
            - Посоветуй менторство""";
      };
    }
  }

  /** Получает дополнительные критерии оценки в зависимости от типа вопроса */
  private String getQuestionTypeCriteria(azhukov.entity.Question.Type questionType) {
    return switch (questionType) {
      case TEXT ->
          """
          ДОПОЛНИТЕЛЬНЫЕ КРИТЕРИИ ДЛЯ ТЕКСТОВЫХ ВОПРОСОВ:
          - Четкость и структурированность ответа
          - Глубина технических знаний
          - Практические примеры и кейсы
          - Логичность изложения
          - Профессиональная терминология""";

      case AUDIO ->
          """
          ДОПОЛНИТЕЛЬНЫЕ КРИТЕРИИ ДЛЯ АУДИО ВОПРОСОВ:
          - Уверенность в голосе и четкость речи
          - Структурированность устного ответа
          - Способность объяснить сложные концепции устно
          - Темп и ритм речи
          - Использование пауз и интонации""";

      case VIDEO ->
          """
          ДОПОЛНИТЕЛЬНЫЕ КРИТЕРИИ ДЛЯ ВИДЕО ВОПРОСОВ:
          - Невербальная коммуникация
          - Уверенность и профессиональный вид
          - Способность презентовать себя
          - Эмоциональная стабильность
          - Культура общения""";

      case CHOICE ->
          """
          ДОПОЛНИТЕЛЬНЫЕ КРИТЕРИИ ДЛЯ ВОПРОСОВ С ВЫБОРОМ:
          - Правильность выбора
          - Обоснование выбора
          - Понимание альтернатив
          - Критическое мышление
          - Логичность рассуждений""";

      default ->
          """
          ОБЩИЕ КРИТЕРИИ:
          - Соответствие ответа вопросу
          - Качество изложения
          - Глубина знаний
          - Практический опыт""";
    };
  }

  /** Парсит оценку из ответа AI */
  private double parseScore(String response) {
    try {
      String cleanResponse = response.trim().replaceAll("[^0-9.]", "");
      double score = Double.parseDouble(cleanResponse);

      // Ограничиваем диапазон 1-10
      if (score < 1) score = 1;
      if (score > 10) score = 10;

      return score;
    } catch (NumberFormatException e) {
      log.error("Failed to parse score from response: {}", response);
      return 0.0;
    }
  }

  /** Определяет результат собеседования на основе среднего балла */
  private Interview.Result determineResult(double averageScore, Double minScore) {
    if (minScore == null) {
      minScore = ApplicationProperties.Constants.DEFAULT_MIN_SCORE;
    }

    return averageScore >= minScore ? Interview.Result.SUCCESSFUL : Interview.Result.UNSUCCESSFUL;
  }
}
