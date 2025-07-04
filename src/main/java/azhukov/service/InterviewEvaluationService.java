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
    log.info("Starting background evaluation of finished interviews");

    try {
      List<Interview> finishedInterviews =
          interviewRepository.findByStatusAndResultIsNullWithAnswers(Interview.Status.FINISHED);

      log.info("Found {} finished interviews to evaluate", finishedInterviews.size());

      for (Interview interview : finishedInterviews) {
        try {
          evaluateInterview(interview);
        } catch (Exception e) {
          log.error("Error evaluating interview {}: {}", interview.getId(), e.getMessage(), e);
        }
      }

      log.info("Background evaluation completed");
    } catch (Exception e) {
      log.error("Error in background evaluation task", e);
    }
  }

  /** Оценивает конкретное собеседование */
  @Transactional
  public void evaluateInterview(Interview interview) {
    log.info("Evaluating interview {}", interview.getId());

    List<InterviewAnswer> answers = interview.getAnswers();
    log.info("Interview {} has {} answers", interview.getId(), answers.size());

    if (answers.isEmpty()) {
      log.warn("Interview {} has no answers to evaluate", interview.getId());

      // Дополнительная диагностика - проверим через репозиторий
      List<InterviewAnswer> answersFromRepo =
          interviewAnswerRepository.findByInterviewId(interview.getId());
      log.info(
          "Interview {} has {} answers from repository", interview.getId(), answersFromRepo.size());

      if (!answersFromRepo.isEmpty()) {
        log.warn(
            "Interview {} has answers in repository but not in entity - possible lazy loading issue",
            interview.getId());
        // Попробуем оценить ответы из репозитория
        for (InterviewAnswer answer : answersFromRepo) {
          try {
            double score = evaluateAnswer(answer, interview.getPosition());
            if (score > 0) {
              answer.setScore(score);
              interviewAnswerRepository.save(answer);
              log.info("Evaluated answer {} from repository with score: {}", answer.getId(), score);
            }
          } catch (Exception e) {
            log.error(
                "Error evaluating answer {} from repository: {}", answer.getId(), e.getMessage());
          }
        }
      }
      return;
    }

    Position position = interview.getPosition();
    List<Double> scores = new ArrayList<>();

    // Оцениваем каждый ответ
    for (InterviewAnswer answer : answers) {
      double score = evaluateAnswer(answer, position);
      if (score > 0) {
        scores.add(score);
        log.debug("Answer {} scored: {}", answer.getId(), score);
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

      log.info(
          "Interview {} evaluated: average score = {}, result = {}",
          interview.getId(),
          averageScore,
          result);
    } else {
      log.warn("No answers were successfully evaluated for interview {}", interview.getId());
    }
  }

  /** Оценивает конкретный ответ через AI */
  public double evaluateAnswer(InterviewAnswer answer, Position position) {
    String answerText =
        answer.getFormattedTranscription() != null
            ? answer.getFormattedTranscription()
            : answer.getAnswerText();

    if (answerText == null || answerText.trim().isEmpty()) {
      log.warn("Answer {} has no text to evaluate", answer.getId());
      return 0.0;
    }

    try {
      String prompt = buildEvaluationPrompt(answerText, position, answer.getQuestion());
      String response = aiService.generateText(prompt);
      double score = parseScore(response);

      // Сохраняем оценку и обоснование
      answer.setScore(score);
      answer.setFeedback(response);

      log.info("Answer {} evaluated with score: {}", answer.getId(), score);
      return score;
    } catch (Exception e) {
      log.error(
          "Error getting evaluation from AI for answer {}: {}", answer.getId(), e.getMessage());
      return 0.0;
    }
  }

  /** Строит промпт для оценки ответа */
  private String buildEvaluationPrompt(
      String answerText, Position position, azhukov.entity.Question question) {
    return String.format(
        """
        Оцените ответ кандидата на вопрос собеседования.

        Позиция: %s
        Уровень: %s
        Вопрос: %s
        Ответ кандидата: %s

        Оцените ответ по шкале от 1 до 10, где:
        - 1-3: Неудовлетворительно
        - 4-6: Удовлетворительно
        - 7-8: Хорошо
        - 9-10: Отлично

        Верните только число от 1 до 10 без дополнительного текста.
        """,
        position.getTitle(), position.getLevel(), question.getText(), answerText);
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
