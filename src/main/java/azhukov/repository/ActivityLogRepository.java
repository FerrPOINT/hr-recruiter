package azhukov.repository;

import azhukov.entity.ActivityLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Репозиторий для работы с лентой активности */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

  /** Получить последние записи активности с фильтрацией по типу */
  @Query(
      "SELECT a FROM ActivityLog a "
          + "WHERE (:type IS NULL OR a.activityType = :type) "
          + "ORDER BY a.createdAt DESC")
  Page<ActivityLog> findRecentActivity(
      @Param("type") ActivityLog.ActivityType type, Pageable pageable);

  /** Получить последние записи активности (без пагинации для виджетов) */
  @Query(
      "SELECT a FROM ActivityLog a "
          + "WHERE (:type IS NULL OR a.activityType = :type) "
          + "ORDER BY a.createdAt DESC")
  List<ActivityLog> findRecentActivityList(
      @Param("type") ActivityLog.ActivityType type, Pageable pageable);

  /** Получить активность по пользователю */
  @Query("SELECT a FROM ActivityLog a " + "WHERE a.userId = :userId " + "ORDER BY a.createdAt DESC")
  Page<ActivityLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

  /** Получить активность по сущности */
  @Query(
      "SELECT a FROM ActivityLog a "
          + "WHERE a.entityType = :entityType AND a.entityId = :entityId "
          + "ORDER BY a.createdAt DESC")
  List<ActivityLog> findByEntity(
      @Param("entityType") ActivityLog.EntityType entityType, @Param("entityId") Long entityId);

  /** Получить количество записей по типу активности */
  @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.activityType = :type")
  long countByActivityType(@Param("type") ActivityLog.ActivityType type);

  /** Получить активность за последние N дней */
  @Query(
      "SELECT a FROM ActivityLog a " + "WHERE a.createdAt >= :since " + "ORDER BY a.createdAt DESC")
  List<ActivityLog> findActivitySince(@Param("since") java.time.LocalDateTime since);
}
