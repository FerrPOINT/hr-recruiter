package azhukov.controller;

import azhukov.api.CandidatesApi;
import azhukov.model.Candidate;
import azhukov.model.CandidateCreateRequest;
import azhukov.model.CandidateUpdateRequest;
import azhukov.service.CandidateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления кандидатами. Реализует интерфейс CandidatesApi, сгенерированный по
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CandidatesApiController implements CandidatesApi {

  private final CandidateService candidateService;

  @Override
  public ResponseEntity<Candidate> createPositionCandidate(
      String positionId, CandidateCreateRequest candidateCreateRequest) {
    log.info("Creating candidate for position: {}", positionId);
    Candidate candidate = candidateService.createCandidate(positionId, candidateCreateRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
  }

  @Override
  public ResponseEntity<Void> deleteCandidate(String id) {
    log.info("Deleting candidate: {}", id);
    candidateService.deleteCandidate(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Candidate> getCandidate(String id) {
    log.info("Getting candidate: {}", id);
    Candidate candidate = candidateService.getCandidateById(id);
    return ResponseEntity.ok(candidate);
  }

  @Override
  public ResponseEntity<List<Candidate>> listPositionCandidates(String positionId) {
    log.info("Getting candidates for position: {}", positionId);
    List<Candidate> candidates = candidateService.getCandidatesByPosition(positionId);
    return ResponseEntity.ok(candidates);
  }

  @Override
  public ResponseEntity<Candidate> updateCandidate(
      String id, CandidateUpdateRequest candidateUpdateRequest) {
    log.info("Updating candidate: {}", id);
    Candidate candidate = candidateService.updateCandidate(id, candidateUpdateRequest);
    return ResponseEntity.ok(candidate);
  }
}
