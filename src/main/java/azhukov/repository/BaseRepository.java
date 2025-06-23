package azhukov.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Базовый репозиторий с общими методами для всех сущностей. Использует JpaSpecificationExecutor для
 * динамических запросов.
 *
 * @param <T> Тип сущности
 * @param <R> Тип ID сущности
 */
@NoRepositoryBean
public interface BaseRepository<T, R extends java.io.Serializable>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

  /** Находит сущность по ID с проверкой на null */
  default T findByIdOrThrow(Long id) {
    return findById(id).orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
  }

  /** Находит сущность по ID с проверкой на null (возвращает Optional) */
  default Optional<T> findByIdOptional(Long id) {
    return findById(id);
  }

  /** Проверяет существование сущности по ID */
  default boolean existsById(Long id) {
    return findById(id).isPresent();
  }

  /** Находит все сущности с пагинацией */
  default Page<T> findAllWithPagination(Pageable pageable) {
    return findAll(pageable);
  }

  /** Находит все активные сущности (если есть поле active) */
  default List<T> findAllActive() {
    // Переопределяется в конкретных репозиториях
    return findAll();
  }

  /** Находит сущности, созданные после указанной даты */
  default List<T> findByCreatedAtAfter(LocalDateTime dateTime) {
    // Переопределяется в конкретных репозиториях
    return findAll();
  }

  /** Находит сущности, созданные в указанном диапазоне дат */
  default List<T> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
    // Переопределяется в конкретных репозиториях
    return findAll();
  }

  /** Подсчитывает количество сущностей */
  default long countAll() {
    return count();
  }

  /** Мягкое удаление (если поддерживается) */
  default void softDelete(Long id) {
    // Переопределяется в конкретных репозиториях
    deleteById(id);
  }

  /** Восстановление мягко удаленной сущности (если поддерживается) */
  default void restore(Long id) {
    // Переопределяется в конкретных репозиториях
    throw new UnsupportedOperationException("Restore not supported for this entity");
  }
}
