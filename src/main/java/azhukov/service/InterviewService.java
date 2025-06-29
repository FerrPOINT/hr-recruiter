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
import azhukov.model.InterviewTranscribeResponse;
import azhukov.model.ListInterviews200Response;
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
  private final WhisperService whisperService;
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
      WhisperService whisperService,
      InterviewMapper interviewMapper,
      CandidateMapper candidateMapper,
      PositionMapper positionMapper,
      QuestionMapper questionMapper) {
    super(interviewRepository);
    this.candidateRepository = candidateRepository;
    this.positionRepository = positionRepository;
    this.questionRepository = questionRepository;
    this.interviewAnswerRepository = interviewAnswerRepository;
    this.whisperService = whisperService;
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
  public ListInterviews200Response listInterviews(
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

    List<azhukov.model.Interview> interviewDtos =
        interviewMapper.toDtoList(interviewsPage.getContent());

    return new ListInterviews200Response()
        .items(interviewDtos)
        .total((long) interviewsPage.getTotalElements());
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

  /** Транскрибирует ответ на вопрос собеседования */
  public InterviewTranscribeResponse transcribeInterviewAnswer(
      Long interviewId, Long questionId, String audioData) {
    log.info("Transcribing answer for interview: {} question: {}", interviewId, questionId);

    try {
      // Получаем интервью
      Interview interview = findByIdOrThrow(interviewId);

      // Проверяем, это первый вопрос?
      List<InterviewAnswer> existingAnswers =
          interviewAnswerRepository.findByInterviewId(interviewId);
      boolean isFirstQuestion = existingAnswers.isEmpty();

      if (isFirstQuestion) {
        // Это первый вопрос - начинаем интервью
        log.info("First question for interview: {}, starting interview", interviewId);
        interview.setStatus(Interview.Status.IN_PROGRESS);

        // Время начала = текущее время - время, выделенное на вопрос
        Position position = interview.getPosition();
        int answerTimeSeconds =
            position.getAnswerTime() != null
                ? position.getAnswerTime()
                : 150; // по умолчанию 2.5 минуты
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(answerTimeSeconds);
        interview.setStartedAt(startTime);

        repository.save(interview);
        log.info("Interview {} started at: {}", interviewId, startTime);
      }

      // Пока используем заглушку для транскрибации
      String transcript = "Transcribed audio content for question " + questionId;

      // Сохраняем ответ в базу используя существующий метод
      interview =
          submitInterviewAnswer(
              interviewId,
              questionId,
              null, // answerText
              null, // audioUrl - сохраняем только транскрипт
              transcript);

      return new InterviewTranscribeResponse().questionId(questionId).transcript(transcript);

    } catch (Exception e) {
      log.error("Error transcribing answer for interview: {}", interviewId, e);
      throw new RuntimeException("Error transcribing audio", e);
    }
  }

  /** Завершает собеседование */
  public Interview finishInterview(Long interviewId) {
    log.info("Finishing interview: {}", interviewId);
    Interview interview = findByIdOrThrow(interviewId);
    if (interview.getStatus() != Interview.Status.IN_PROGRESS) {
      throw new ValidationException("Собеседование не может быть завершено в текущем статусе");
    }
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
    boolean alreadyAnswered =
        interview.getAnswers().stream()
            .anyMatch(answer -> answer.getQuestion().getId().equals(questionId));
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

    if (answeredQuestions >= totalQuestions) {
      // Это последний ответ - завершаем собеседование
      log.info("Last answer submitted for interview: {}, finishing interview", interviewId);
      finishInterview(interviewId);
    }

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

    List<Interview> allInterviews = repository.findAll();
    int total = allInterviews.size();
    int successful = (int) allInterviews.stream().filter(Interview::isSuccessful).count();
    int unsuccessful = total - successful;

    return InterviewStats.builder()
        .total(total)
        .successful(successful)
        .unsuccessful(unsuccessful)
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

  /** Находит ответ на вопрос по ID */
  @Transactional(readOnly = true)
  public InterviewAnswer findAnswerById(Long answerId) {
    log.info("Finding answer by ID: {}", answerId);
    return interviewAnswerRepository
        .findById(answerId)
        .orElseThrow(() -> new ResourceNotFoundException("Ответ не найден: " + answerId));
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
