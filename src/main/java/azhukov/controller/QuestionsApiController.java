package azhukov.controller;

import azhukov.api.QuestionsApi;
import azhukov.model.BaseQuestionFields;
import azhukov.model.GetAllQuestions200Response;
import azhukov.model.Question;
import azhukov.model.QuestionCreateRequest;
import azhukov.service.QuestionService;
import azhukov.util.PageableUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления вопросами собеседований. Реализует интерфейс QuestionsApi,
 * сгенерированный по OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class QuestionsApiController implements QuestionsApi {

  private final QuestionService questionService;

  @Override
  public ResponseEntity<GetAllQuestions200Response> getAllQuestions(
      Optional<Long> page, Optional<Long> size, Optional<String> sort) {

    log.debug("Получение списка всех вопросов: page={}, size={}, sort={}", page, size, sort);

    // Используем утилитный метод для создания Pageable
    Pageable pageable =
        PageableUtils.fromOptionals(page.map(Long::intValue), size.map(Long::intValue), sort);
    Page<Question> questionPage = questionService.getAllQuestions(pageable);

    // Преобразуем Page<Question> в GetAllQuestions200Response
    GetAllQuestions200Response response = new GetAllQuestions200Response();
    response.setContent(questionPage.getContent());
    response.setTotalElements((long) questionPage.getTotalElements());
    response.setTotalPages((long) questionPage.getTotalPages());
    response.setSize((long) questionPage.getSize());
    response.setNumber((long) questionPage.getNumber());

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Question> getQuestion(Long id) {
    log.debug("Получение вопроса по ID: {}", id);

    Question question = questionService.getQuestionById(id);
    return ResponseEntity.ok(question);
  }

  @Override
  public ResponseEntity<List<Question>> listPositionQuestions(Long positionId) {
    log.debug("Получение списка вопросов для вакансии: {}", positionId);

    List<Question> questions = questionService.getQuestionsByPosition(positionId);
    return ResponseEntity.ok(questions);
  }

  @Override
  public ResponseEntity<Question> createPositionQuestion(
      Long positionId, QuestionCreateRequest request) {
    log.debug("Создание вопроса для вакансии {}: {}", positionId, request);

    Question question = questionService.createQuestion(positionId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(question);
  }

  @Override
  public ResponseEntity<Question> updateQuestion(Long id, BaseQuestionFields body) {
    log.debug("Обновление вопроса {}: {}", id, body);

    Question question = questionService.updateQuestion(id, body);
    return ResponseEntity.ok(question);
  }

  @Override
  public ResponseEntity<Void> deleteQuestion(Long id) {
    log.debug("Удаление вопроса: {}", id);

    questionService.deleteQuestion(id);
    return ResponseEntity.noContent().build();
  }
}
