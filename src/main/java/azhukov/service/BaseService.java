package azhukov.service;

import azhukov.exception.ResourceNotFoundException;
import azhukov.repository.BaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Базовый сервис с общими методами для всех сервисов. Предоставляет стандартные CRUD операции и
 * логирование.
 *
 * @param <T> Тип сущности
 * @param <ID> Тип ID сущности
 * @param <R> Тип репозитория
 */
@Slf4j
public abstract class BaseService<T, ID, R extends BaseRepository<T, ID>> {

  protected final R repository;

  protected BaseService(R repository) {
    this.repository = repository;
  }

  /** Находит сущность по ID */
  @Transactional(readOnly = true)
  public Optional<T> findById(ID id) {
    log.debug("Finding entity by id: {}", id);
    return repository.findById(id);
  }

  /** Находит сущность по ID или выбрасывает исключение */
  @Transactional(readOnly = true)
  public T findByIdOrThrow(ID id) {
    log.debug("Finding entity by id: {}", id);
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));
  }

  /** Находит все сущности */
  @Transactional(readOnly = true)
  public List<T> findAll() {
    log.debug("Finding all entities");
    return repository.findAll();
  }

  /** Находит все сущности с пагинацией */
  @Transactional(readOnly = true)
  public Page<T> findAll(Pageable pageable) {
    log.debug("Finding all entities with pagination: {}", pageable);
    return repository.findAll(pageable);
  }

  /** Сохраняет сущность */
  @Transactional
  public T save(T entity) {
    log.debug("Saving entity: {}", entity);
    return repository.save(entity);
  }

  /** Сохраняет список сущностей */
  @Transactional
  public List<T> saveAll(List<T> entities) {
    log.debug("Saving {} entities", entities.size());
    return repository.saveAll(entities);
  }

  /** Обновляет сущность */
  @Transactional
  public T update(ID id, T entity) {
    log.debug("Updating entity with id: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Entity not found with id: " + id);
    }
    return repository.save(entity);
  }

  /** Удаляет сущность по ID */
  @Transactional
  public void deleteById(ID id) {
    log.debug("Deleting entity with id: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Entity not found with id: " + id);
    }
    repository.deleteById(id);
  }

  /** Проверяет существование сущности по ID */
  @Transactional(readOnly = true)
  public boolean existsById(ID id) {
    return repository.existsById(id);
  }

  /** Подсчитывает количество сущностей */
  @Transactional(readOnly = true)
  public long count() {
    return repository.count();
  }

  /** Находит сущности, созданные после указанной даты */
  @Transactional(readOnly = true)
  public List<T> findByCreatedAtAfter(LocalDateTime dateTime) {
    log.debug("Finding entities created after: {}", dateTime);
    return repository.findByCreatedAtAfter(dateTime);
  }

  /** Находит сущности в указанном диапазоне дат создания */
  @Transactional(readOnly = true)
  public List<T> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
    log.debug("Finding entities created between {} and {}", start, end);
    return repository.findByCreatedAtBetween(start, end);
  }

  /** Мягкое удаление сущности */
  @Transactional
  public void softDelete(ID id) {
    log.debug("Soft deleting entity with id: {}", id);
    repository.softDelete(id);
  }

  /** Восстановление мягко удаленной сущности */
  @Transactional
  public void restore(ID id) {
    log.debug("Restoring entity with id: {}", id);
    repository.restore(id);
  }

  /** Получает репозиторий для специфических операций */
  protected R getRepository() {
    return repository;
  }
}
