package azhukov.controller;

import azhukov.api.InterviewsApi;
import azhukov.entity.Interview;
import azhukov.mapper.InterviewMapper;
import azhukov.model.*;
import azhukov.service.InterviewService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления собеседованиями. Реализует интерфейс InterviewsApi, сгенерированный из
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class InterviewsApiController implements InterviewsApi {

  private final InterviewService interviewService;
  private final InterviewMapper interviewMapper;

  @Override
  public ResponseEntity<azhukov.model.Interview> createInterviewFromCandidate(Long candidateId) {
    log.info("Creating interview for candidate: {}", candidateId);

    try {
      Interview interview = interviewService.createInterviewFromCandidate(candidateId);
      azhukov.model.Interview interviewDto = interviewMapper.toDto(interview);
      return ResponseEntity.status(HttpStatus.CREATED).body(interviewDto);
    } catch (Exception e) {
      log.error("Error creating interview for candidate: {}", candidateId, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<azhukov.model.Interview> finishInterview(Long id) {
    log.info("Finishing interview: {}", id);

    try {
      Interview interview = interviewService.finishInterview(id);
      azhukov.model.Interview interviewDto = interviewMapper.toDto(interview);
      return ResponseEntity.ok(interviewDto);
    } catch (Exception e) {
      log.error("Error finishing interview: {}", id, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<List<GetChecklist200ResponseInner>> getChecklist() {
    log.info("Getting interview checklist");

    // Заглушка для чек-листа
    List<GetChecklist200ResponseInner> checklist =
        List.of(
            new GetChecklist200ResponseInner().text("Подготовить вопросы для собеседования"),
            new GetChecklist200ResponseInner().text("Проверить техническое оборудование"),
            new GetChecklist200ResponseInner().text("Подготовить комнату для собеседования"),
            new GetChecklist200ResponseInner().text("Уведомить кандидата о времени"),
            new GetChecklist200ResponseInner().text("Подготовить документы для подписания"));

    return ResponseEntity.ok(checklist);
  }

  @Override
  public ResponseEntity<azhukov.model.Interview> getInterview(Long id) {
    log.info("Getting interview: {}", id);

    try {
      Interview interview = interviewService.findByIdOrThrow(id);
      azhukov.model.Interview interviewDto = interviewMapper.toDto(interview);
      return ResponseEntity.ok(interviewDto);
    } catch (Exception e) {
      log.error("Error getting interview: {}", id, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<GetInviteInfo200Response> getInviteInfo() {
    log.info("Getting invite info");

    // Заглушка для информации о приглашении
    GetInviteInfo200Response inviteInfo =
        new GetInviteInfo200Response().language("Русский").questionsCount((long) 5);

    return ResponseEntity.ok(inviteInfo);
  }

  @Override
  public ResponseEntity<ListInterviews200Response> listInterviews(
      Optional<Long> positionId,
      Optional<Long> candidateId,
      Optional<Long> page,
      Optional<Long> size) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}, size: {}",
        positionId.orElse(null),
        candidateId.orElse(null),
        page.orElse(1L),
        size.orElse(20L));

    try {
      int pageNum = page.orElse(1L).intValue();
      int pageSize = size.orElse(20L).intValue();
      PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);
      Page<Interview> interviewsPage =
          interviewService.listInterviews(
              positionId.orElse(null), candidateId.orElse(null), pageRequest);

      List<azhukov.model.Interview> interviewDtos =
          interviewMapper.toDtoList(interviewsPage.getContent());

      ListInterviews200Response response =
          new ListInterviews200Response()
              .items(interviewDtos)
              .total((long) interviewsPage.getTotalElements());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error listing interviews", e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<List<azhukov.model.Interview>> listPositionInterviews(Long positionId) {
    log.info("Listing interviews for position: {}", positionId);

    try {
      List<Interview> interviews = interviewService.getInterviewsByPosition(positionId);
      List<azhukov.model.Interview> interviewDtos = interviewMapper.toDtoList(interviews);
      return ResponseEntity.ok(interviewDtos);
    } catch (Exception e) {
      log.error("Error listing interviews for position: {}", positionId, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<Void> startInterview(Long id) {
    log.info("Starting interview: {}", id);
    try {
      interviewService.startInterview(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Error starting interview: {}", id, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<azhukov.model.Interview> submitInterviewAnswer(
      Long id, InterviewAnswerCreateRequest interviewAnswerCreateRequest) {
    log.info(
        "Submitting answer for interview: {} question: {}",
        id,
        interviewAnswerCreateRequest.getQuestionId());
    try {
      Long questionId = interviewAnswerCreateRequest.getQuestionId();
      Interview interview =
          interviewService.submitInterviewAnswer(
              id,
              questionId,
              interviewAnswerCreateRequest.getAnswerText(),
              interviewAnswerCreateRequest.getAudioUrl(),
              interviewAnswerCreateRequest.getTranscript());
      azhukov.model.Interview interviewDto = interviewMapper.toDto(interview);
      return ResponseEntity.ok(interviewDto);
    } catch (Exception e) {
      log.error("Error submitting answer for interview: {}", id, e);
      throw e;
    }
  }
}
