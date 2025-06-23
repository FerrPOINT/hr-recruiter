package azhukov.service;

import azhukov.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Базовый сервис с общими методами для всех сервисов. Предоставляет стандартные CRUD операции и
 * логирование.
 *
 * @param <T> Тип сущности
 * @param <R> Тип репозитория
 */
@Slf4j
public abstract class BaseService<
    T, R extends java.io.Serializable, REPO extends JpaRepository<T, Long>> {

  protected final REPO repository;

  protected BaseService(REPO repository) {
    this.repository = repository;
  }

  /** Находит сущность по ID */
  @Transactional(readOnly = true)
  public Optional<T> findById(Long id) {
    log.debug("Finding entity by id: {}", id);
    return repository.findById(id);
  }

  /** Находит сущность по ID или выбрасывает исключение */
  @Transactional(readOnly = true)
  public T findByIdOrThrow(Long id) {
    log.debug("Finding entity by id: {}", id);
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Not found: " + id));
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
  public T update(Long id, T entity) {
    log.debug("Updating entity with id: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Entity not found with id: " + id);
    }
    return repository.save(entity);
  }

  /** Удаляет сущность по ID */
  @Transactional
  public void deleteById(Long id) {
    log.debug("Deleting entity with id: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Entity not found with id: " + id);
    }
    repository.deleteById(id);
  }

  /** Проверяет существование сущности по ID */
  @Transactional(readOnly = true)
  public boolean existsById(Long id) {
    return repository.existsById(id);
  }

  /** Подсчитывает количество сущностей */
  @Transactional(readOnly = true)
  public long count() {
    return repository.count();
  }

  /** Получает репозиторий для специфических операций */
  protected REPO getRepository() {
    return repository;
  }
}
