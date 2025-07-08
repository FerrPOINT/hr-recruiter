package azhukov.controller;

import azhukov.api.CandidatesApi;
import azhukov.model.Candidate;
import azhukov.model.CandidateAuthRequest;
import azhukov.model.CandidateAuthResponse;
import azhukov.model.CandidateCreateRequest;
import azhukov.model.CandidateUpdateRequest;
import azhukov.model.PaginatedResponse;
import azhukov.service.CandidateService;
import azhukov.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления кандидатами. Реализует интерфейс CandidatesApi, сгенерированный по
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CandidatesApiController extends BaseController implements CandidatesApi {

  private final CandidateService candidateService;

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<Candidate> createPositionCandidate(
      Long positionId, CandidateCreateRequest candidateCreateRequest) {
    log.info("Creating candidate for position: {}", positionId);
    Candidate candidate = candidateService.createCandidate(positionId, candidateCreateRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteCandidate(Long id) {
    log.info("Deleting candidate: {}", id);
    candidateService.deleteCandidate(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<Candidate> getCandidate(Long id) {
    log.info("Getting candidate: {}", id);
    Candidate candidate = candidateService.getCandidateById(id);
    return ResponseEntity.ok(candidate);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<List<Candidate>> listPositionCandidates(Long positionId) {
    log.info("Getting candidates for position: {}", positionId);
    List<Candidate> candidates = candidateService.getCandidatesByPosition(positionId);
    return ResponseEntity.ok(candidates);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PaginatedResponse> listCandidates(
      Optional<Long> positionId,
      Optional<String> search,
      Optional<Long> page,
      Optional<Long> size,
      Optional<String> sort) {
    log.debug(
        "Getting candidates with positionId={}, search={}, page={}, size={}, sort={}",
        positionId,
        search,
        page,
        size,
        sort);

    Pageable pageable = PaginationUtils.createPageableFromOptional(page, size);

    Page<Candidate> candidates =
        candidateService.getCandidatesPage(positionId.orElse(null), search.orElse(null), pageable);

    PaginatedResponse response = new PaginatedResponse();
    PaginationUtils.fillPaginationFields(candidates, response);

    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Candidate> updateCandidate(
      Long id, CandidateUpdateRequest candidateUpdateRequest) {
    log.info("Updating candidate: {}", id);
    Candidate candidate = candidateService.updateCandidate(id, candidateUpdateRequest);
    return ResponseEntity.ok(candidate);
  }

  @Override
  public ResponseEntity<CandidateAuthResponse> authCandidate(
      @Valid @RequestBody CandidateAuthRequest candidateAuthRequest) {
    CandidateAuthResponse response =
        candidateService.findOrCreateCandidateAndJwt(candidateAuthRequest);
    return ResponseEntity.ok(response);
  }
}
