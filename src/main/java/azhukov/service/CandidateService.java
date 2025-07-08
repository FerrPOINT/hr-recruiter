package azhukov.service;

import azhukov.entity.Candidate;
import azhukov.entity.Position;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.CandidateMapper;
import azhukov.model.CandidateAuthRequest;
import azhukov.model.CandidateAuthResponse;
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
  private final JwtService jwtService;

  /** Получает всех кандидатов с пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Candidate> getAllCandidates(Pageable pageable) {
    log.debug("Getting all candidates with pageable: {}", pageable);
    Page<Candidate> candidates = candidateRepository.findAll(pageable);
    return candidates.map(candidateMapper::toDto);
  }

  /** Получает кандидатов по вакансии */
  @Transactional(readOnly = true)
  public List<azhukov.model.Candidate> getCandidatesByPosition(Long positionId) {
    log.debug("Getting candidates for position: {}", positionId);
    List<Candidate> candidates = candidateRepository.findByPositionId(positionId);
    return candidateMapper.toDtoList(candidates);
  }

  /** Получает кандидатов с пагинацией и фильтрацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Candidate> getCandidatesPage(
      Long positionId, String search, Pageable pageable) {
    log.debug(
        "Getting candidates page with positionId={}, search={}, pageable={}",
        positionId,
        search,
        pageable);

    Page<Candidate> candidates;

    if (positionId != null && search != null && !search.trim().isEmpty()) {
      // Фильтр по вакансии и поиск - используем только фильтр по вакансии для простоты
      Position position =
          positionRepository
              .findById(positionId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Position not found: " + positionId));
      candidates = candidateRepository.findByPosition(position, pageable);
    } else if (positionId != null) {
      // Только фильтр по вакансии
      Position position =
          positionRepository
              .findById(positionId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Position not found: " + positionId));
      candidates = candidateRepository.findByPosition(position, pageable);
    } else if (search != null && !search.trim().isEmpty()) {
      // Только поиск
      candidates = candidateRepository.findBySearchTerm(search, pageable);
    } else {
      // Без фильтров
      candidates = candidateRepository.findAll(pageable);
    }

    return candidates.map(candidateMapper::toDto);
  }

  /** Создает нового кандидата для вакансии */
  @Transactional
  public azhukov.model.Candidate createCandidate(
      Long positionId, azhukov.model.CandidateCreateRequest request) {
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
  public azhukov.model.Candidate getCandidateById(Long id) {
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
      Long id, azhukov.model.CandidateUpdateRequest request) {
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
  public void deleteCandidate(Long id) {
    log.debug("Deleting candidate: {}", id);
    candidateRepository.deleteById(id);
  }

  /** Находит или создает кандидата по email/телефону */
  @Transactional
  public Candidate findOrCreateCandidate(
      String firstName, String lastName, String email, String phone, Long positionId) {
    Candidate candidate = null;
    if (email != null && !email.isBlank()) {
      candidate = candidateRepository.findByEmail(email);
    }
    if (candidate == null && phone != null && !phone.isBlank()) {
      candidate = candidateRepository.findByPhone(phone);
    }
    if (candidate != null) {
      return candidate;
    }
    // Создать нового кандидата
    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));
    candidate =
        Candidate.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .phone(phone)
            .position(position)
            .status(Candidate.Status.NEW)
            .build();
    return candidateRepository.save(candidate);
  }

  public CandidateAuthResponse findOrCreateCandidateAndJwt(CandidateAuthRequest request) {
    // 1. Найти или создать кандидата
    Candidate candidateEntity =
        findOrCreateCandidate(
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPhone(),
            request.getPositionId());
    // 2. Сгенерировать JWT
    String token = jwtService.generateCandidateToken(candidateEntity);
    // 3. Преобразовать entity в DTO
    azhukov.model.Candidate candidateDto = candidateMapper.toDto(candidateEntity);
    // 4. Вернуть DTO-ответ
    CandidateAuthResponse response = new CandidateAuthResponse();
    response.setToken(token);
    response.setCandidate(candidateDto);
    return response;
  }
}
