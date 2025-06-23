package azhukov.controller;

import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.service.BaseService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Базовый контроллер с общими методами для всех контроллеров. Предоставляет стандартные CRUD
 * операции и обработку ошибок.
 *
 * @param <T> Тип сущности
 * @param <ID> Тип ID сущности
 * @param <S> Тип сервиса
 */
@Slf4j
public abstract class BaseController<T, ID, S extends BaseService<T, ID, ?>> {

  protected final S service;

  protected BaseController(S service) {
    this.service = service;
  }

  /** Получает сущность по ID */
  @GetMapping("/{id}")
  public ResponseEntity<T> getById(@PathVariable ID id) {
    log.debug("Getting entity by id: {}", id);
    try {
      T entity = service.findByIdOrThrow(id);
      return ResponseEntity.ok(entity);
    } catch (ResourceNotFoundException e) {
      log.warn("Entity not found with id: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error getting entity by id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Получает все сущности */
  @GetMapping
  public ResponseEntity<List<T>> getAll() {
    log.debug("Getting all entities");
    try {
      List<T> entities = service.findAll();
      return ResponseEntity.ok(entities);
    } catch (Exception e) {
      log.error("Error getting all entities", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Получает все сущности с пагинацией */
  @GetMapping("/page")
  public ResponseEntity<Page<T>> getAllPaginated(Pageable pageable) {
    log.debug("Getting all entities with pagination: {}", pageable);
    try {
      Page<T> page = service.findAll(pageable);
      return ResponseEntity.ok(page);
    } catch (Exception e) {
      log.error("Error getting paginated entities", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Создает новую сущность */
  @PostMapping
  public ResponseEntity<T> create(@RequestBody T entity) {
    log.debug("Creating new entity: {}", entity);
    try {
      T savedEntity = service.save(entity);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
    } catch (ValidationException e) {
      log.warn("Validation error creating entity: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error creating entity", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Обновляет сущность по ID */
  @PutMapping("/{id}")
  public ResponseEntity<T> update(@PathVariable ID id, @RequestBody T entity) {
    log.debug("Updating entity with id: {}", id);
    try {
      T updatedEntity = service.update(id, entity);
      return ResponseEntity.ok(updatedEntity);
    } catch (ResourceNotFoundException e) {
      log.warn("Entity not found for update with id: {}", id);
      return ResponseEntity.notFound().build();
    } catch (ValidationException e) {
      log.warn("Validation error updating entity: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error updating entity with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Удаляет сущность по ID */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable ID id) {
    log.debug("Deleting entity with id: {}", id);
    try {
      service.deleteById(id);
      return ResponseEntity.noContent().build();
    } catch (ResourceNotFoundException e) {
      log.warn("Entity not found for deletion with id: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error deleting entity with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Мягкое удаление сущности по ID */
  @DeleteMapping("/{id}/soft")
  public ResponseEntity<Void> softDelete(@PathVariable ID id) {
    log.debug("Soft deleting entity with id: {}", id);
    try {
      service.softDelete(id);
      return ResponseEntity.noContent().build();
    } catch (ResourceNotFoundException e) {
      log.warn("Entity not found for soft deletion with id: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error soft deleting entity with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Восстанавливает мягко удаленную сущность по ID */
  @PostMapping("/{id}/restore")
  public ResponseEntity<Void> restore(@PathVariable ID id) {
    log.debug("Restoring entity with id: {}", id);
    try {
      service.restore(id);
      return ResponseEntity.ok().build();
    } catch (ResourceNotFoundException e) {
      log.warn("Entity not found for restoration with id: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error restoring entity with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Проверяет существование сущности по ID */
  @GetMapping("/{id}/exists")
  public ResponseEntity<Boolean> exists(@PathVariable ID id) {
    log.debug("Checking existence of entity with id: {}", id);
    try {
      boolean exists = service.existsById(id);
      return ResponseEntity.ok(exists);
    } catch (Exception e) {
      log.error("Error checking existence of entity with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Подсчитывает количество сущностей */
  @GetMapping("/count")
  public ResponseEntity<Long> count() {
    log.debug("Counting entities");
    try {
      long count = service.count();
      return ResponseEntity.ok(count);
    } catch (Exception e) {
      log.error("Error counting entities", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Получает сервис для специфических операций */
  protected S getService() {
    return service;
  }
}
