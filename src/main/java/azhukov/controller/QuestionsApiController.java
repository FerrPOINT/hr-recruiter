package azhukov.controller;

import azhukov.api.QuestionsApi;
import azhukov.mapper.QuestionMapper;
import azhukov.model.BaseQuestionFields;
import azhukov.model.ListPositionQuestions200Response;
import azhukov.model.PaginatedResponse;
import azhukov.model.PositionQuestionsResponse;
import azhukov.model.QuestionCreateRequest;
import azhukov.service.QuestionService;
import azhukov.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления вопросами собеседований. Реализует интерфейс QuestionsApi,
 * сгенерированный по OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class QuestionsApiController implements QuestionsApi {

  private final QuestionService questionService;
  private final QuestionMapper questionMapper;

  @Override
  public ResponseEntity<PaginatedResponse> getAllQuestions(
      Optional<Long> page, Optional<Long> size, Optional<String> sort) {
    log.debug("Получение списка всех вопросов");
    Pageable pageable = PaginationUtils.createPageableFromOptional(page, size, sort, "createdAt");

    Page<azhukov.model.Question> questionDtos = questionService.getAllQuestions(pageable);

    azhukov.model.PaginatedResponse response = new azhukov.model.PaginatedResponse();
    PaginationUtils.fillPaginationFields(questionDtos, response);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<azhukov.model.Question> getQuestion(Long id) {
    log.debug("Получение вопроса по ID: {}", id);
    azhukov.model.Question question = questionService.getQuestionById(id);
    return ResponseEntity.ok(question);
  }

  @Override
  public ResponseEntity<azhukov.model.Question> updateQuestion(Long id, BaseQuestionFields body) {
    log.debug("Обновление вопроса по ID: {}", id);
    azhukov.model.Question question = questionService.updateQuestion(id, body);
    return ResponseEntity.ok(question);
  }

  @Override
  public ResponseEntity<Void> deleteQuestion(Long id) {
    log.debug("Удаление вопроса по ID: {}", id);
    questionService.deleteQuestion(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ListPositionQuestions200Response> listPositionQuestions(Long positionId) {
    log.debug("Получение списка вопросов по вакансии: {}", positionId);

    List<azhukov.model.Question> questions = questionService.getQuestionsByPosition(positionId);

    // Создаем Page объект для использования с PaginationUtils
    Page<azhukov.model.Question> questionPage =
        new PageImpl<>(questions, PageRequest.of(0, questions.size()), questions.size());

    ListPositionQuestions200Response response = new ListPositionQuestions200Response();
    PaginationUtils.fillPaginationFields(questionPage, response);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<azhukov.model.Question> createPositionQuestion(
      Long positionId, QuestionCreateRequest questionCreateRequest) {
    log.debug("Добавление вопроса к вакансии: {}", positionId);
    azhukov.model.Question question =
        questionService.createQuestion(positionId, questionCreateRequest);
    return ResponseEntity.status(201).body(question);
  }

  @Override
  public ResponseEntity<PositionQuestionsResponse> getPositionQuestionsWithSettings(
      Long positionId) {
    log.debug("Получение вопросов с настройками для вакансии: {}", positionId);
    PositionQuestionsResponse response =
        questionService.getPositionQuestionsResponseWithSettings(positionId);
    return ResponseEntity.ok(response);
  }
}
