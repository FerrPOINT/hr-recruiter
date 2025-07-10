package azhukov.service;

import azhukov.entity.Candidate;
import azhukov.entity.Interview;
import azhukov.entity.InterviewAnswer;
import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.mapper.CandidateMapper;
import azhukov.mapper.InterviewMapper;
import azhukov.mapper.PositionMapper;
import azhukov.mapper.QuestionMapper;
import azhukov.model.GetInterview200Response;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import azhukov.repository.QuestionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class InterviewService extends BaseService<Interview, Long, InterviewRepository> {

  private final CandidateRepository candidateRepository;
  private final PositionRepository positionRepository;
  private final QuestionRepository questionRepository;
  private final InterviewAnswerRepository interviewAnswerRepository;
  private final InterviewMapper interviewMapper;
  private final CandidateMapper candidateMapper;
  private final PositionMapper positionMapper;
  private final QuestionMapper questionMapper;

  public InterviewService(
      InterviewRepository interviewRepository,
      CandidateRepository candidateRepository,
      PositionRepository positionRepository,
      QuestionRepository questionRepository,
      InterviewAnswerRepository interviewAnswerRepository,
      InterviewMapper interviewMapper,
      CandidateMapper candidateMapper,
      PositionMapper positionMapper,
      QuestionMapper questionMapper) {
    super(interviewRepository);
    this.candidateRepository = candidateRepository;
    this.positionRepository = positionRepository;
    this.questionRepository = questionRepository;
    this.interviewAnswerRepository = interviewAnswerRepository;
    this.interviewMapper = interviewMapper;
    this.candidateMapper = candidateMapper;
    this.positionMapper = positionMapper;
    this.questionMapper = questionMapper;
  }

  /** Создает новое собеседование для кандидата и возвращает DTO */
  public azhukov.model.Interview createInterviewFromCandidate(Long candidateId) {
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

    return interviewMapper.toDto(savedInterview);
  }

  /** Получает детальную информацию о собеседовании */
  public GetInterview200Response getInterviewDetails(Long id) {
    log.info("Getting interview details: {}", id);

    Interview interview = findByIdOrThrow(id);

    // Получаем связанные данные
    var candidate = candidateMapper.toDto(interview.getCandidate());
    var position = positionMapper.toDto(interview.getPosition());

    // Получаем вопросы для вакансии и преобразуем в DTO
    List<Question> questions = questionRepository.findByPosition(interview.getPosition());
    List<azhukov.model.Question> questionDtos = questionMapper.toDtoList(questions);

    var response =
        new GetInterview200Response()
            .interview(interviewMapper.toDto(interview))
            .candidate(candidate)
            .position(position)
            .questions(questionDtos);

    return response;
  }

  /** Получает список собеседований с пагинацией и возвращает DTO */
  public Page<azhukov.model.Interview> listInterviews(
      Long positionId, Long candidateId, Long page, Long size) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}, size: {}",
        positionId,
        candidateId,
        page,
        size);

    int pageNum = page.intValue();
    int pageSize = size.intValue();
    PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);

    Page<Interview> interviewsPage = listInterviews(positionId, candidateId, pageRequest);
    return interviewsPage.map(interviewMapper::toDto);
  }

  /** Получает собеседования по вакансии и возвращает DTO */
  public List<azhukov.model.Interview> getInterviewsByPosition(Long positionId) {
    log.info("Getting interviews for position: {}", positionId);

    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Вакансия не найдена: " + positionId));

    List<Interview> interviews = repository.findByPosition(position);
    return interviewMapper.toDtoList(interviews);
  }

  /** Получает собеседования с пагинацией и фильтрацией, возвращает Page<DTO> */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Interview> getInterviewsPage(
      Long positionId, Long candidateId, Pageable pageable) {
    log.debug(
        "Getting interviews page with positionId={}, candidateId={}, pageable={}",
        positionId,
        candidateId,
        pageable);

    Page<Interview> interviews = listInterviews(positionId, candidateId, pageable);
    return interviews.map(interviewMapper::toDto);
  }

  /** Завершает собеседование */
  /** Начинает собеседование */
  public Interview startInterview(Long interviewId) {
    log.info("Starting interview: {}", interviewId);
    Interview interview = findByIdOrThrow(interviewId);

    if (interview.getStatus() != Interview.Status.NOT_STARTED) {
      throw new ValidationException("Собеседование уже начато или завершено");
    }

    // Устанавливаем статус в процессе
    interview.setStatus(Interview.Status.IN_PROGRESS);
    interview.setStartedAt(LocalDateTime.now());

    Interview savedInterview = repository.save(interview);
    log.info("Interview {} started", interviewId);
    return savedInterview;
  }

  /** Завершает собеседование вручную */
  public Interview finishInterview(Long interviewId) {
    log.info("Finishing interview: {}", interviewId);
    Interview interview = findByIdOrThrow(interviewId);
    if (interview.getStatus() != Interview.Status.IN_PROGRESS) {
      throw new ValidationException("Собеседование не может быть завершено в текущем статусе");
    }

    // Проверяем, есть ли ответы для оценки
    List<InterviewAnswer> answers = interviewAnswerRepository.findByInterviewId(interviewId);
    if (answers.isEmpty()) {
      log.warn("Interview {} has no answers to evaluate, setting result to ERROR", interviewId);
      interview.setStatus(Interview.Status.FINISHED);
      interview.setResult(Interview.Result.ERROR);
      interview.setFinishedAt(LocalDateTime.now());
      Interview savedInterview = repository.save(interview);
      log.info("Interview finished with ERROR result: {}", interviewId);
      return savedInterview;
    }

    // Определяем результат на основе оценок (если они есть)
    Interview.Result result = determineInterviewResult(interview);
    interview.finish(result);
    updateCandidateStatus(interview.getCandidate(), result);
    Interview savedInterview = repository.save(interview);
    log.info("Interview finished: {} with result: {}", interviewId, result);
    return savedInterview;
  }

  /** Добавляет ответ на вопрос собеседования */
  public Interview submitInterviewAnswer(
      Long interviewId,
      Long questionId,
      String answerText,
      String audioUrl,
      String rawTranscription) {
    log.info("Submitting answer for interview: {} question: {}", interviewId, questionId);
    Interview interview = findByIdOrThrow(interviewId);
    if (interview.getStatus() != Interview.Status.IN_PROGRESS
        && interview.getStatus() != Interview.Status.NOT_STARTED) {
      throw new ValidationException("Ответ можно добавить только к активному собеседованию");
    }
    Question question =
        questionRepository
            .findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Вопрос не найден: " + questionId));
    if (!question.getPosition().getId().equals(interview.getPosition().getId())) {
      throw new ValidationException("Вопрос не принадлежит вакансии собеседования");
    }
    // Проверяем, не отвечен ли уже этот вопрос
    boolean alreadyAnswered =
        interviewAnswerRepository.existsByInterviewIdAndQuestionId(interviewId, questionId);
    if (alreadyAnswered) {
      throw new ValidationException("На этот вопрос уже дан ответ");
    }

    // Создаем ответ
    InterviewAnswer answer =
        InterviewAnswer.builder()
            .interview(interview)
            .question(question)
            .answerText(answerText)
            .audioUrl(audioUrl)
            .rawTranscription(rawTranscription)
            .build();
    interview.addAnswer(answer);

    // Проверяем, это последний вопрос?
    Position position = interview.getPosition();
    int totalQuestions = position.getQuestionsCount();
    int answeredQuestions = interview.getAnswers().size();

    Interview savedInterview = repository.save(interview);
    log.info(
        "Answer submitted for interview: {} question: {} (answered: {}/{})",
        interviewId,
        questionId,
        answeredQuestions,
        totalQuestions);
    return savedInterview;
  }

  /** Получает список собеседований с фильтрацией */
  @Transactional(readOnly = true)
  public Page<Interview> listInterviews(Long positionId, Long candidateId, Pageable pageable) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}",
        positionId,
        candidateId,
        pageable.getPageNumber());

    if (positionId != null && candidateId != null) {
      return repository.findByPositionId(positionId, pageable);
    } else if (positionId != null) {
      return repository.findByPositionId(positionId, pageable);
    } else if (candidateId != null) {
      return repository.findByCandidateId(candidateId, pageable);
    } else {
      return repository.findAll(pageable);
    }
  }

  /** Получает собеседования по кандидату */
  @Transactional(readOnly = true)
  public List<Interview> getInterviewsByCandidate(Long candidateId) {
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

    // Используем оптимизированные запросы вместо загрузки всех данных
    long total = repository.count();
    long successful = repository.countByResult(Interview.Result.SUCCESSFUL);
    long unsuccessful = repository.countByResult(Interview.Result.UNSUCCESSFUL);
    long error = repository.countByResult(Interview.Result.ERROR);

    return InterviewStats.builder()
        .total((int) total)
        .successful((int) successful)
        .unsuccessful((int) (unsuccessful + error)) // Включаем ошибки в неуспешные
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
    // Получаем ответы напрямую из репозитория для точного подсчета
    List<InterviewAnswer> answers = interviewAnswerRepository.findByInterviewId(interview.getId());

    if (answers.isEmpty()) {
      log.warn("Interview {} has no answers, setting result to ERROR", interview.getId());
      return Interview.Result.ERROR;
    }

    // Проверяем, есть ли оценки
    List<InterviewAnswer> scoredAnswers =
        answers.stream().filter(answer -> answer.getScore() != null).toList();

    if (scoredAnswers.isEmpty()) {
      log.warn(
          "Interview {} has answers but no scores, setting result to ERROR", interview.getId());
      return Interview.Result.ERROR;
    }

    // Вычисляем среднюю оценку
    double averageScore =
        scoredAnswers.stream().mapToDouble(answer -> answer.getScore()).average().orElse(0.0);

    log.info("Interview {} average score: {}", interview.getId(), averageScore);

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

  /** Находит ответ на вопрос по ID */
  @Transactional(readOnly = true)
  public InterviewAnswer findAnswerById(Long answerId) {
    log.info("Finding answer by ID: {}", answerId);
    return interviewAnswerRepository
        .findById(answerId)
        .orElseThrow(() -> new ResourceNotFoundException("Ответ не найден: " + answerId));
  }

  /** Получает маппер для преобразования в DTO */
  public InterviewMapper getInterviewMapper() {
    return interviewMapper;
  }

  /** Получает чек-лист для интервью */
  public List<azhukov.model.GetChecklist200ResponseInner> getInterviewChecklist() {
    log.info("Getting interview checklist");

    List<azhukov.model.GetChecklist200ResponseInner> checklist =
        List.of(
            new azhukov.model.GetChecklist200ResponseInner()
                .text("Проверить техническое оборудование"),
            new azhukov.model.GetChecklist200ResponseInner().text("Подготовить вопросы"),
            new azhukov.model.GetChecklist200ResponseInner().text("Проверить связь"),
            new azhukov.model.GetChecklist200ResponseInner()
                .text("Подготовить документы кандидата"),
            new azhukov.model.GetChecklist200ResponseInner().text("Настроить запись"));

    return checklist;
  }

  /** Получает информацию для приглашения */
  public azhukov.model.GetInviteInfo200Response getInviteInfo() {
    log.info("Getting invite information");

    // Получаем общую статистику по вопросам и языкам
    long totalQuestions = questionRepository.count();
    String defaultLanguage = "Русский"; // Можно сделать настраиваемым

    return new azhukov.model.GetInviteInfo200Response()
        .questionsCount(totalQuestions)
        .language(defaultLanguage);
  }

  /** Отправляет ответ на вопрос интервью */
  public Interview submitInterviewAnswer(
      Long interviewId, azhukov.model.InterviewAnswerCreateRequest request) {
    log.info("Submitting interview answer for interview: {}", interviewId);

    Interview interview = findByIdOrThrow(interviewId);

    if (interview.getStatus() != Interview.Status.IN_PROGRESS) {
      throw new ValidationException("Интервью должно быть в процессе для отправки ответов");
    }

    // Извлекаем данные из запроса
    Long questionId = request.getQuestionId();
    String answerText = request.getAnswerText();
    String audioUrl = request.getAudioUrl();
    String transcript = request.getTranscript();

    // Вызываем существующий метод
    return submitInterviewAnswer(interviewId, questionId, answerText, audioUrl, transcript);
  }

  /** Статистика по собеседованиям */
  @lombok.Data
  @lombok.Builder
  public static class InterviewStats {
    private int total;
    private int successful;
    private int unsuccessful;
  }
}
