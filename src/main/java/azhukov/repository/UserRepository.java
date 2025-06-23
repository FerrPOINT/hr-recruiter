package azhukov.repository;

import azhukov.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с пользователями. Расширяет BaseRepository для автоматизации общих
 * операций.
 */
@Repository
public interface UserRepository extends BaseRepository<UserEntity, String> {

  /** Находит пользователя по email */
  Optional<UserEntity> findByEmail(String email);

  /** Находит пользователя по email (без Optional) */
  UserEntity findByEmailIgnoreCase(String email);

  /** Проверяет существование пользователя по email */
  boolean existsByEmail(String email);

  /** Находит пользователей по роли */
  List<UserEntity> findByRole(UserEntity.Role role);

  /** Находит активных пользователей */
  @Query("SELECT u FROM UserEntity u WHERE u.active = true")
  List<UserEntity> findAllActive();

  /** Находит пользователей по роли с пагинацией */
  Page<UserEntity> findByRole(UserEntity.Role role, Pageable pageable);

  /** Находит пользователей по части имени или email */
  @Query(
      "SELECT u FROM UserEntity u WHERE "
          + "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<UserEntity> findBySearchTerm(@Param("search") String search, Pageable pageable);

  /** Находит пользователей по языку */
  List<UserEntity> findByLanguage(String language);

  /** Находит пользователей, созданных после указанной даты */
  @Query("SELECT u FROM UserEntity u WHERE u.createdAt >= :dateTime")
  List<UserEntity> findByCreatedAtAfter(@Param("dateTime") java.time.LocalDateTime dateTime);

  /** Находит пользователей в указанном диапазоне дат создания */
  @Query("SELECT u FROM UserEntity u WHERE u.createdAt BETWEEN :start AND :end")
  List<UserEntity> findByCreatedAtBetween(
      @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

  /** Подсчитывает количество пользователей по роли */
  long countByRole(UserEntity.Role role);

  /** Подсчитывает количество активных пользователей */
  @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.active = true")
  long countActive();

  /** Находит пользователей по списку ID */
  @Query("SELECT u FROM UserEntity u WHERE u.id IN :ids")
  List<UserEntity> findByIds(@Param("ids") List<String> ids);

  /** Мягкое удаление пользователя (устанавливает active = false) */
  @Query("UPDATE UserEntity u SET u.active = false WHERE u.id = :id")
  void softDelete(@Param("id") String id);

  /** Восстановление пользователя (устанавливает active = true) */
  @Query("UPDATE UserEntity u SET u.active = true WHERE u.id = :id")
  void restore(@Param("id") String id);
}
