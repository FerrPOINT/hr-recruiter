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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления собеседованиями. Реализует интерфейс InterviewsApi, сгенерированный из
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class InterviewsApiController extends BaseController implements InterviewsApi {

  private final InterviewService interviewService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Interview> createInterviewFromCandidate(Long candidateId) {
    log.info("Creating interview for candidate: {}", candidateId);
    azhukov.model.Interview interviewDto =
        interviewService.createInterviewFromCandidate(candidateId);
    return ResponseEntity.status(HttpStatus.CREATED).body(interviewDto);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<GetInterview200Response> getInterview(Long id) {
    log.info("Getting interview information: {}", id);
    GetInterview200Response response = interviewService.getInterviewDetails(id);
    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
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

    Pageable pageable = PaginationUtils.createPageableFromOptional(page, size);

    Page<azhukov.model.Interview> interviews =
        interviewService.getInterviewsPage(
            positionId.orElse(null), candidateId.orElse(null), pageable);

    PaginatedResponse response = new PaginatedResponse();
    PaginationUtils.fillPaginationFields(interviews, response);

    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<azhukov.model.Interview>> listPositionInterviews(Long positionId) {
    log.info("Listing interviews for position: {}", positionId);
    List<azhukov.model.Interview> interviewDtos =
        interviewService.getInterviewsByPosition(positionId);
    return ResponseEntity.ok(interviewDtos);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Interview> finishInterview(Long id) {
    log.info("Manually finishing interview: {}", id);
    azhukov.entity.Interview finishedInterview = interviewService.finishInterview(id);
    azhukov.model.Interview interviewDto =
        interviewService.getInterviewMapper().toDto(finishedInterview);
    return ResponseEntity.ok(interviewDto);
  }
}
