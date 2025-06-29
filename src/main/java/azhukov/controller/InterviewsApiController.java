package azhukov.controller;

import azhukov.api.InterviewsApi;
import azhukov.model.*;
import azhukov.service.InterviewService;
import azhukov.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Override
  public ResponseEntity<azhukov.model.Interview> createInterviewFromCandidate(Long candidateId) {
    log.info("Creating interview for candidate: {}", candidateId);
    try {
      azhukov.model.Interview interviewDto =
          interviewService.createInterviewFromCandidate(candidateId);
      return ResponseEntity.status(HttpStatus.CREATED).body(interviewDto);
    } catch (Exception e) {
      log.error("Error creating interview for candidate: {}", candidateId, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<GetInterview200Response> getInterview(Long id) {
    log.info("Getting interview information: {}", id);
    try {
      GetInterview200Response response = interviewService.getInterviewDetails(id);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error getting interview information: {}", id, e);
      throw e;
    }
  }

  @Override
  public ResponseEntity<PaginatedResponse> listInterviews(
      Optional<Long> positionId,
      Optional<Long> candidateId,
      Optional<Long> page,
      Optional<Long> size,
      Optional<String> sort) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}, size: {}, sort: {}",
        positionId.orElse(null),
        candidateId.orElse(null),
        page.orElse(0L),
        size.orElse(20L),
        sort.orElse("createdAt"));

    try {
      Long pageNum = page.orElse(0L);
      Long pageSize = size.orElse(20L);
      Pageable pageable = PaginationUtils.createPageableFromOptional(page, size);

      Page<azhukov.model.Interview> interviews =
          interviewService.getInterviewsPage(
              positionId.orElse(null), candidateId.orElse(null), pageable);

      PaginatedResponse response = new PaginatedResponse();
      PaginationUtils.fillPaginationFields(interviews, response);

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
      List<azhukov.model.Interview> interviewDtos =
          interviewService.getInterviewsByPosition(positionId);
      return ResponseEntity.ok(interviewDtos);
    } catch (Exception e) {
      log.error("Error listing interviews for position: {}", positionId, e);
      throw e;
    }
  }
}
