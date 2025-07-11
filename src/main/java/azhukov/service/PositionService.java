package azhukov.service;

import azhukov.entity.Position;
import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.PositionMapper;
import azhukov.model.*;
import azhukov.repository.PositionRepository;
import azhukov.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для управления вакансиями. Расширяет BaseService для автоматизации общих операций. */
@Service
@Slf4j
public class PositionService extends BaseService<Position, Long, PositionRepository> {

  private final PositionMapper positionMapper;
  private final UserRepository userRepository;

  public PositionService(
      PositionRepository positionRepository,
      PositionMapper positionMapper,
      UserRepository userRepository) {
    super(positionRepository);
    this.positionMapper = positionMapper;
    this.userRepository = userRepository;
  }

  /** Создает новую вакансию */
  @Transactional
  public azhukov.model.Position createPosition(PositionCreateRequest request, String userEmail) {
    log.info("Creating new position: {}", request.getTitle());

    UserEntity createdBy =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + userEmail));

    Position position = positionMapper.toEntity(request);
    position.setCreatedBy(createdBy);

    // Заполняем команду, если указана
    if (request.getTeam() != null && !request.getTeam().isEmpty()) {
      List<Long> teamIds =
          request.getTeam().stream().map(Long::parseLong).collect(Collectors.toList());
      List<UserEntity> team = userRepository.findByIds(teamIds);
      position.setTeam(team);
    }

    Position savedPosition = repository.save(position);
    return positionMapper.toDto(savedPosition);
  }

  /** Обновляет вакансию */
  @Transactional
  public azhukov.model.Position updatePosition(Long id, PositionUpdateRequest request) {
    log.info("Updating position: {}", id);

    Position position = findByIdOrThrow(id);
    positionMapper.updateEntityFromRequest(request, position);

    // Обновляем команду, если указана
    if (request.getTeam() != null) {
      List<Long> teamIds =
          request.getTeam().stream().map(Long::parseLong).collect(Collectors.toList());
      List<UserEntity> team = userRepository.findByIds(teamIds);
      position.setTeam(team);
    }

    Position updatedPosition = repository.save(position);
    return positionMapper.toDto(updatedPosition);
  }

  /** Частично обновляет вакансию */
  @Transactional
  public azhukov.model.Position partialUpdatePosition(
      Long id, PartialUpdatePositionRequest request) {
    log.info("Partially updating position: {}", id);

    Position position = findByIdOrThrow(id);

    if (request.getStatus() != null) {
      position.setStatus(positionMapper.mapStatus(request.getStatus()));
    }

    Position updatedPosition = repository.save(position);
    return positionMapper.toDto(updatedPosition);
  }

  /** Получает вакансию по ID */
  @Transactional(readOnly = true)
  public azhukov.model.Position getPositionById(Long id) {
    log.debug("Getting position by id: {}", id);
    Position position =
        repository
            .findByIdWithInterviewsAndCandidates(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found with id: " + id));

    // Вручную инициализируем team (одна коллекция, не вызывает MultipleBagFetchException)
    org.hibernate.Hibernate.initialize(position.getTeam());

    return positionMapper.toDto(position);
  }

  /** Получает все вакансии */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getAllPositions() {
    log.debug("Getting all positions");
    List<Position> positions = repository.findAll();
    return positionMapper.toDtoList(positions);
  }

  /** Получает все вакансии с пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Position> getAllPositions(Pageable pageable) {
    log.debug("Getting all positions with pagination");
    Page<Position> positions = repository.findAll(pageable);
    return positionMapper.toDtoPage(positions);
  }

  /** Получает вакансии с фильтрацией и пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Position> getPositionsPage(
      PositionStatusEnum status, String search, String owner, String userEmail, Pageable pageable) {
    log.debug(
        "Getting positions with filters: status={}, search={}, owner={}", status, search, owner);

    // Определяем, нужно ли фильтровать по владельцу
    boolean filterByOwner = "me".equals(owner) && userEmail != null;

    if (filterByOwner) {
      UserEntity currentUser =
          userRepository
              .findByEmail(userEmail)
              .orElseThrow(
                  () -> new ResourceNotFoundException("User not found with email: " + userEmail));

      if (status != null && search != null && !search.trim().isEmpty()) {
        Position.Status entityStatus = positionMapper.mapStatus(status);
        Page<Position> positions =
            repository.findByCreatedByAndStatusAndSearch(
                currentUser, entityStatus, search, pageable);
        return positionMapper.toDtoPage(positions);
      } else if (status != null) {
        Position.Status entityStatus = positionMapper.mapStatus(status);
        Page<Position> positions =
            repository.findByCreatedByAndStatus(currentUser, entityStatus, pageable);
        return positionMapper.toDtoPage(positions);
      } else if (search != null && !search.trim().isEmpty()) {
        Page<Position> positions =
            repository.findByCreatedByAndSearch(currentUser, search, pageable);
        return positionMapper.toDtoPage(positions);
      } else {
        Page<Position> positions = repository.findByCreatedBy(currentUser, pageable);
        return positionMapper.toDtoPage(positions);
      }
    } else {
      // Фильтрация по всем вакансиям (owner = "all" или не указан)
      if (status != null && search != null && !search.trim().isEmpty()) {
        Position.Status entityStatus = positionMapper.mapStatus(status);
        Page<Position> positions = repository.findByStatusAndSearch(entityStatus, search, pageable);
        return positionMapper.toDtoPage(positions);
      } else if (status != null) {
        Position.Status entityStatus = positionMapper.mapStatus(status);
        // Используем только fetch интервью
        List<Position> positions = repository.findAllWithInterviews();
        // Пагинация вручную (если нужно)
        // TODO: если нужна пагинация, реализовать вручную или через отдельный запрос
        return positionMapper.toDtoPage(new org.springframework.data.domain.PageImpl<>(positions));
      } else if (search != null && !search.trim().isEmpty()) {
        Page<Position> positions = repository.findBySearchTerm(search, pageable);
        return positionMapper.toDtoPage(positions);
      } else {
        List<Position> positions = repository.findAllWithInterviews();
        return positionMapper.toDtoPage(new org.springframework.data.domain.PageImpl<>(positions));
      }
    }
  }

  /** Получает вакансии по статусу */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByStatus(PositionStatusEnum status) {
    log.debug("Getting positions by status: {}", status);
    Position.Status entityStatus = positionMapper.mapStatus(status);
    // Используем только fetch интервью
    List<Position> positions = repository.findAllWithInterviews();
    return positionMapper.toDtoList(positions);
  }

  /** Получает вакансии по статусу с пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Position> getPositionsByStatus(
      PositionStatusEnum status, Pageable pageable) {
    log.debug("Getting positions by status with pagination: {}", status);
    Position.Status entityStatus = positionMapper.mapStatus(status);
    Page<Position> positions = repository.findByStatus(entityStatus, pageable);
    return positionMapper.toDtoPage(positions);
  }

  /** Получает вакансии по компании */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByCompany(String company) {
    log.debug("Getting positions by company: {}", company);
    // Используем только fetch интервью
    List<Position> positions = repository.findAllWithInterviews();
    return positionMapper.toDtoList(positions);
  }

  /** Получает вакансии по создателю */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByCreator(UserEntity createdBy) {
    log.debug("Getting positions by creator: {}", createdBy.getId());
    // Используем только fetch интервью
    List<Position> positions = repository.findAllWithInterviews();
    return positionMapper.toDtoList(positions);
  }

  /** Ищет вакансии по поисковому запросу */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Position> searchPositions(String search, Pageable pageable) {
    log.debug("Searching positions with term: {}", search);
    Page<Position> positions = repository.findBySearchTerm(search, pageable);
    return positionMapper.toDtoPage(positions);
  }

  /** Ищет вакансии по статусу и поисковому запросу */
  @Transactional(readOnly = true)
  public Page<azhukov.model.Position> searchPositionsByStatus(
      PositionStatusEnum status, String search, Pageable pageable) {
    log.debug("Searching positions by status and term: {} - {}", status, search);
    Position.Status entityStatus = positionMapper.mapStatus(status);
    Page<Position> positions = repository.findByStatusAndSearch(entityStatus, search, pageable);
    return positionMapper.toDtoPage(positions);
  }

  /** Получает вакансии по теме */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByTopic(String topic) {
    log.debug("Getting positions by topic: {}", topic);
    List<Position> positions = repository.findByTopic(topic);
    return positionMapper.toDtoList(positions);
  }

  /** Получает вакансии с минимальным баллом */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByMinScore(Double minScore) {
    log.debug("Getting positions by min score: {}", minScore);
    List<Position> positions = repository.findByMinScoreGreaterThan(minScore);
    return positionMapper.toDtoList(positions);
  }

  /** Получает вакансии по члену команды */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsByTeamMember(String userId) {
    log.debug("Getting positions by team member: {}", userId);
    List<Position> positions = repository.findByTeamMember(userId);
    return positionMapper.toDtoList(positions);
  }

  /** Получает вакансии с публичными ссылками */
  @Transactional(readOnly = true)
  public List<azhukov.model.Position> getPositionsWithPublicLinks() {
    log.debug("Getting positions with public links");
    List<Position> positions = repository.findByPublicLinkIsNotNull();
    return positionMapper.toDtoList(positions);
  }

  /** Мягко удаляет вакансию (архивирует) */
  @Transactional
  public void softDeletePosition(Long id) {
    log.info("Soft deleting position: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Position not found with id: " + id);
    }
    Position position = findByIdOrThrow(id);
    position.setStatus(Position.Status.ARCHIVED);
    repository.save(position);
  }

  /** Восстанавливает вакансию из архива */
  @Transactional
  public void restorePosition(Long id) {
    log.info("Restoring position: {}", id);
    Position position = findByIdOrThrow(id);
    position.setStatus(Position.Status.ACTIVE);
    repository.save(position);
  }

  /** Обновляет статус вакансии */
  @Transactional
  public azhukov.model.Position updatePositionStatus(Long id, PositionStatusEnum status) {
    log.info("Updating position status: {} -> {}", id, status);
    Position position = findByIdOrThrow(id);
    position.setStatus(positionMapper.mapStatus(status));
    Position updatedPosition = repository.save(position);
    return positionMapper.toDto(updatedPosition);
  }

  /** Подсчитывает вакансии по статусу */
  @Transactional(readOnly = true)
  public long countPositionsByStatus(PositionStatusEnum status) {
    log.debug("Counting positions by status: {}", status);
    Position.Status entityStatus = positionMapper.mapStatus(status);
    return repository.countByStatus(entityStatus);
  }

  /** Подсчитывает вакансии по компании */
  @Transactional(readOnly = true)
  public long countPositionsByCompany(String company) {
    log.debug("Counting positions by company: {}", company);
    return repository.countByCompany(company);
  }

  /** Подсчитывает вакансии по создателю */
  @Transactional(readOnly = true)
  public long countPositionsByCreator(UserEntity createdBy) {
    log.debug("Counting positions by creator: {}", createdBy.getId());
    return repository.countByCreatedBy(createdBy);
  }

  /** Получает публичную ссылку на вакансию */
  @Transactional(readOnly = true)
  public String getPositionPublicLink(Long id) {
    log.debug("Getting public link for position: {}", id);
    Position position = findByIdOrThrow(id);
    return position.getPublicLink();
  }

  /** Получает статистику по вакансии */
  @Transactional(readOnly = true)
  public Position getPositionStats(Long positionId) {
    log.debug("Getting stats for position: {}", positionId);
    return findByIdOrThrow(positionId);
  }
}
