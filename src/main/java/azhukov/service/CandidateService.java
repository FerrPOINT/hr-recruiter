package azhukov.service;

import azhukov.entity.Candidate;
import azhukov.entity.Position;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.CandidateMapper;
import azhukov.repository.CandidateRepository;
import azhukov.repository.PositionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidateService {

  private final CandidateRepository candidateRepository;
  private final PositionRepository positionRepository;
  private final CandidateMapper candidateMapper;

  /** Получает всех кандидатов с пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Candidate> getAllCandidates(Pageable pageable) {
    log.debug("Getting all candidates with pageable: {}", pageable);
    Page<Candidate> candidates = candidateRepository.findAll(pageable);
    return candidates.map(candidateMapper::toDto);
  }

  /** Получает кандидатов по вакансии */
  @Transactional(readOnly = true)
  public List<azhukov.model.Candidate> getCandidatesByPosition(String positionId) {
    log.debug("Getting candidates for position: {}", positionId);
    List<Candidate> candidates = candidateRepository.findByPositionId(positionId);
    return candidateMapper.toDtoList(candidates);
  }

  /** Создает нового кандидата для вакансии */
  @Transactional
  public azhukov.model.Candidate createCandidate(
      String positionId, azhukov.model.CandidateCreateRequest request) {
    log.debug("Creating candidate for position: {}", positionId);

    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));

    Candidate entity = candidateMapper.toEntity(request);
    entity.setPosition(position);

    Candidate savedEntity = candidateRepository.save(entity);
    return candidateMapper.toDto(savedEntity);
  }

  /** Получает кандидата по ID */
  @Transactional(readOnly = true)
  public azhukov.model.Candidate getCandidateById(String id) {
    log.debug("Getting candidate by id: {}", id);
    Candidate entity =
        candidateRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
    return candidateMapper.toDto(entity);
  }

  /** Обновляет кандидата */
  @Transactional
  public azhukov.model.Candidate updateCandidate(
      String id, azhukov.model.CandidateUpdateRequest request) {
    log.debug("Updating candidate with id: {}", id);

    Candidate entity =
        candidateRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));

    candidateMapper.updateEntityFromRequest(request, entity);

    Candidate savedEntity = candidateRepository.save(entity);
    return candidateMapper.toDto(savedEntity);
  }

  /** Удаляет кандидата */
  @Transactional
  public void deleteCandidate(String id) {
    log.debug("Deleting candidate with id: {}", id);
    Candidate entity =
        candidateRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
    candidateRepository.delete(entity);
  }
}
