package azhukov.service;

import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.QuestionMapper;
import azhukov.repository.PositionRepository;
import azhukov.repository.QuestionRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления вопросами собеседований. Расширяет BaseService для автоматизации общих
 * операций.
 */
@Service
@Slf4j
@Transactional
public class QuestionService extends BaseService<Question, Long, QuestionRepository> {

  private final QuestionMapper questionMapper;
  private final PositionRepository positionRepository;

  public QuestionService(
      QuestionRepository questionRepository,
      QuestionMapper questionMapper,
      PositionRepository positionRepository) {
    super(questionRepository);
    this.questionMapper = questionMapper;
    this.positionRepository = positionRepository;
  }

  public Page<azhukov.model.Question> getAllQuestions(Pageable pageable) {
    log.debug("Getting all questions with pageable: {}", pageable);
    Page<Question> questions = repository.findAll(pageable);
    return questions.map(questionMapper::toDto);
  }

  /** Создает новый вопрос для вакансии */
  @Transactional
  public azhukov.model.Question createQuestion(
      Long positionId, azhukov.model.QuestionCreateRequest request) {
    log.debug("Creating question for position: {}", positionId);

    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));

    Question entity = questionMapper.toEntity(request);
    entity.setPosition(position);

    Question savedEntity = repository.save(entity);
    return questionMapper.toDto(savedEntity);
  }

  /** Обновляет вопрос */
  @Transactional
  public azhukov.model.Question updateQuestion(Long id, azhukov.model.BaseQuestionFields request) {
    log.debug("Updating question: {}", id);

    Question entity = findByIdOrThrow(id);

    // Обновляем поля из request
    if (request.getText() != null) {
      entity.setText(request.getText());
    }
    if (request.getType() != null) {
      entity.setType(questionMapper.mapType(request.getType()));
    }
    if (request.getOrder() != null) {
      entity.setOrder(request.getOrder().intValue());
    }
    if (request.getIsRequired() != null) {
      entity.setRequired(request.getIsRequired());
    }

    Question updatedEntity = repository.save(entity);
    return questionMapper.toDto(updatedEntity);
  }

  /** Получает вопросы по вакансии */
  @Transactional(readOnly = true)
  public List<azhukov.model.Question> getQuestionsByPosition(Long positionId) {
    log.debug("Getting questions for position: {}", positionId);
    List<Question> questions = repository.findByPositionId(positionId);
    return questionMapper.toDtoList(questions);
  }

  /** Получает вопросы по вакансии, отсортированные по порядку */
  @Transactional(readOnly = true)
  public List<azhukov.model.Question> getQuestionsByPositionOrdered(Long positionId) {
    log.debug("Getting ordered questions for position: {}", positionId);
    List<Question> questions = repository.findByPositionIdOrderByOrderAsc(positionId);
    return questionMapper.toDtoList(questions);
  }

  /** Получает вопрос по ID */
  @Transactional(readOnly = true)
  public azhukov.model.Question getQuestionById(Long id) {
    log.debug("Getting question by id: {}", id);
    Question question = findByIdOrThrow(id);
    return questionMapper.toDto(question);
  }

  /** Удаляет вопрос */
  @Transactional
  public void deleteQuestion(Long id) {
    log.debug("Deleting question: {}", id);
    Question question = findByIdOrThrow(id);
    repository.delete(question);
  }

  @Transactional(readOnly = true)
  public List<Question> getPositionQuestions(Long positionId) {
    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Position not found with id: " + positionId));
    return repository.findByPositionOrderByOrderAsc(position);
  }

  /** Возвращает вопросы по вакансии в формате PositionQuestionsResponse (без настроек) */
  public azhukov.model.PositionQuestionsResponse getPositionQuestionsResponse(Long positionId) {
    List<azhukov.model.Question> questions = getQuestionsByPosition(positionId);
    azhukov.model.PositionQuestionsResponse response =
        new azhukov.model.PositionQuestionsResponse();
    response.setQuestions(questions);
    // Можно не заполнять interviewSettings, если не требуется
    return response;
  }

  /** Возвращает вопросы по вакансии с настройками интервью */
  public azhukov.model.PositionQuestionsResponse getPositionQuestionsResponseWithSettings(
      Long positionId) {
    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));
    List<azhukov.model.Question> questions = getQuestionsByPosition(positionId);
    azhukov.model.PositionQuestionsResponse response =
        new azhukov.model.PositionQuestionsResponse();
    response.setQuestions(questions);
    // Используем маппер для настройки интервью
    azhukov.model.PositionQuestionsResponseInterviewSettings settings =
        questionMapper.toInterviewSettings(position);
    response.setInterviewSettings(settings);
    return response;
  }
}
