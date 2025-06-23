package azhukov.service;

import azhukov.entity.*;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import azhukov.repository.QuestionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления собеседованиями. Обеспечивает полный жизненный цикл собеседований от
 * создания до завершения.
 */
@Service
@Slf4j
@Transactional
public class InterviewService extends BaseService<Interview, String, InterviewRepository> {

  private final CandidateRepository candidateRepository;
  private final PositionRepository positionRepository;
  private final QuestionRepository questionRepository;

  public InterviewService(
      InterviewRepository interviewRepository,
      CandidateRepository candidateRepository,
      PositionRepository positionRepository,
      QuestionRepository questionRepository) {
    super(interviewRepository);
    this.candidateRepository = candidateRepository;
    this.positionRepository = positionRepository;
    this.questionRepository = questionRepository;
  }

  /** Создает новое собеседование для кандидата */
  public Interview createInterviewFromCandidate(String candidateId) {
    log.info("Creating interview for candidate: {}", candidateId);

    Candidate candidate =
        candidateRepository
            .findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Кандидат не найден: " + candidateId));

    // Проверяем, нет ли уже активного собеседования у кандидата
    if (candidate.hasActiveInterview()) {
      throw new ValidationException("У кандидата уже есть активное собеседование");
    }

    Interview interview =
        Interview.builder()
            .candidate(candidate)
            .position(candidate.getPosition())
            .status(Interview.Status.NOT_STARTED)
            .build();

    Interview savedInterview = repository.save(interview);
    log.info("Created interview: {} for candidate: {}", savedInterview.getId(), candidateId);

    return savedInterview;
  }

  /** Начинает собеседование */
  public void startInterview(String interviewId) {
    log.info("Starting interview: {}", interviewId);

    Interview interview = findByIdOrThrow(interviewId);

    if (interview.getStatus() != Interview.Status.NOT_STARTED) {
      throw new ValidationException("Собеседование уже начато или завершено");
    }

    interview.start();
    repository.save(interview);

    log.info("Interview started: {}", interviewId);
  }

  /** Завершает собеседование */
  public Interview finishInterview(String interviewId) {
    log.info("Finishing interview: {}", interviewId);

    Interview interview = findByIdOrThrow(interviewId);

    if (interview.getStatus() != Interview.Status.IN_PROGRESS) {
      throw new ValidationException("Собеседование не может быть завершено в текущем статусе");
    }

    // Определяем результат на основе оценок
    Interview.Result result = determineInterviewResult(interview);
    interview.finish(result);

    // Обновляем статус кандидата
    updateCandidateStatus(interview.getCandidate(), result);

    Interview savedInterview = repository.save(interview);
    log.info("Interview finished: {} with result: {}", interviewId, result);

    return savedInterview;
  }

  /** Добавляет ответ на вопрос собеседования */
  public Interview submitInterviewAnswer(
      String interviewId,
      String questionId,
      String answerText,
      String audioUrl,
      String transcript) {
    log.info("Submitting answer for interview: {} question: {}", interviewId, questionId);

    Interview interview = findByIdOrThrow(interviewId);

    if (interview.getStatus() != Interview.Status.IN_PROGRESS) {
      throw new ValidationException("Ответ можно добавить только к активному собеседованию");
    }

    Question question =
        questionRepository
            .findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Вопрос не найден: " + questionId));

    // Проверяем, что вопрос принадлежит вакансии собеседования
    if (!question.getPosition().getId().equals(interview.getPosition().getId())) {
      throw new ValidationException("Вопрос не принадлежит вакансии собеседования");
    }

    // Проверяем, что на этот вопрос еще не отвечали
    boolean alreadyAnswered =
        interview.getAnswers().stream()
            .anyMatch(answer -> answer.getQuestion().getId().equals(questionId));

    if (alreadyAnswered) {
      throw new ValidationException("На этот вопрос уже дан ответ");
    }

    InterviewAnswer answer =
        InterviewAnswer.builder()
            .interview(interview)
            .question(question)
            .answerText(answerText)
            .audioUrl(audioUrl)
            .transcript(transcript)
            .build();

    interview.addAnswer(answer);

    Interview savedInterview = repository.save(interview);
    log.info("Answer submitted for interview: {} question: {}", interviewId, questionId);

    return savedInterview;
  }

  /** Получает список собеседований с фильтрацией */
  @Transactional(readOnly = true)
  public Page<Interview> listInterviews(String positionId, String candidateId, Pageable pageable) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}",
        positionId,
        candidateId,
        pageable.getPageNumber());

    if (positionId != null && candidateId != null) {
      // Используем существующие методы репозитория
      return repository.findByPositionId(UUID.fromString(positionId), pageable);
    } else if (positionId != null) {
      return repository.findByPositionId(UUID.fromString(positionId), pageable);
    } else if (candidateId != null) {
      return repository.findByCandidateId(candidateId, pageable);
    } else {
      return repository.findAll(pageable);
    }
  }

  /** Получает собеседования по вакансии */
  @Transactional(readOnly = true)
  public List<Interview> getInterviewsByPosition(String positionId) {
    log.info("Getting interviews for position: {}", positionId);

    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Вакансия не найдена: " + positionId));

    return repository.findByPosition(position);
  }

  /** Получает собеседования по кандидату */
  @Transactional(readOnly = true)
  public List<Interview> getInterviewsByCandidate(String candidateId) {
    log.info("Getting interviews for candidate: {}", candidateId);

    Candidate candidate =
        candidateRepository
            .findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Кандидат не найден: " + candidateId));

    return repository.findByCandidate(candidate);
  }

  /** Получает активные собеседования */
  @Transactional(readOnly = true)
  public List<Interview> getActiveInterviews() {
    log.info("Getting active interviews");
    return repository.findActiveInterviews();
  }

  /** Получает завершенные собеседования */
  @Transactional(readOnly = true)
  public List<Interview> getFinishedInterviews() {
    log.info("Getting finished interviews");
    return repository.findFinishedInterviews();
  }

  /** Получает успешные собеседования */
  @Transactional(readOnly = true)
  public List<Interview> getSuccessfulInterviews() {
    log.info("Getting successful interviews");
    return repository.findSuccessfulInterviews();
  }

  /** Получает статистику по собеседованиям */
  @Transactional(readOnly = true)
  public InterviewStats getInterviewStats() {
    log.info("Getting interview statistics");

    long total = repository.count();
    long successful = repository.countSuccessful();
    long unsuccessful = repository.countUnsuccessful();
    long inProgress = repository.countByStatus(Interview.Status.IN_PROGRESS);

    return InterviewStats.builder()
        .total((int) total)
        .successful((int) successful)
        .unsuccessful((int) unsuccessful)
        .inProgress((int) inProgress)
        .build();
  }

  /** Получает статистику по собеседованиям за период */
  @Transactional(readOnly = true)
  public List<Interview> getInterviewsByDateRange(LocalDateTime start, LocalDateTime end) {
    log.info("Getting interviews between {} and {}", start, end);
    return repository.findByStartedAtBetween(start, end);
  }

  /** Определяет результат собеседования на основе оценок */
  private Interview.Result determineInterviewResult(Interview interview) {
    Double averageScore = interview.getAverageAnswerScore();

    if (averageScore == null) {
      return Interview.Result.UNSUCCESSFUL;
    }

    // Если средняя оценка выше 70, считаем собеседование успешным
    return averageScore >= 70.0 ? Interview.Result.SUCCESSFUL : Interview.Result.UNSUCCESSFUL;
  }

  /** Обновляет статус кандидата на основе результата собеседования */
  private void updateCandidateStatus(Candidate candidate, Interview.Result result) {
    if (result == Interview.Result.SUCCESSFUL) {
      candidate.setStatus(Candidate.Status.HIRED);
    } else {
      candidate.setStatus(Candidate.Status.REJECTED);
    }
    candidateRepository.save(candidate);
  }

  /** Статистика по собеседованиям */
  @lombok.Data
  @lombok.Builder
  public static class InterviewStats {
    private int total;
    private int successful;
    private int unsuccessful;
    private int inProgress;
  }
}
