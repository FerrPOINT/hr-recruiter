package azhukov.repository;

import azhukov.entity.Position;
import azhukov.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с вакансиями. Расширяет BaseRepository для автоматизации общих операций.
 */
@Repository
public interface PositionRepository extends BaseRepository<Position, Long> {

  /** Находит все вакансии с загрузкой интервью */
  @Override
  @EntityGraph(attributePaths = {"interviews"})
  List<Position> findAll();

  /** Находит все вакансии с пагинацией и загрузкой интервью */
  @Override
  @EntityGraph(attributePaths = {"interviews"})
  Page<Position> findAll(Pageable pageable);

  /** Находит вакансии по статусу */
  @EntityGraph(attributePaths = {"interviews"})
  List<Position> findByStatus(Position.Status status);

  /** Находит вакансии по статусу с пагинацией */
  @EntityGraph(attributePaths = {"interviews"})
  Page<Position> findByStatus(Position.Status status, Pageable pageable);

  /** Находит вакансии по компании */
  @EntityGraph(attributePaths = {"interviews"})
  List<Position> findByCompany(String company);

  /** Находит вакансии по создателю */
  @EntityGraph(attributePaths = {"interviews"})
  List<Position> findByCreatedBy(UserEntity createdBy);

  /** Находит вакансии по создателю с пагинацией */
  @EntityGraph(attributePaths = {"interviews"})
  Page<Position> findByCreatedBy(UserEntity createdBy, Pageable pageable);

  /** Находит вакансии по создателю и статусу */
  @EntityGraph(attributePaths = {"interviews"})
  Page<Position> findByCreatedByAndStatus(
      UserEntity createdBy, Position.Status status, Pageable pageable);

  /** Находит вакансии по создателю и поисковому запросу */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.createdBy = :createdBy AND "
          + "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.company) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Position> findByCreatedByAndSearch(
      @Param("createdBy") UserEntity createdBy, @Param("search") String search, Pageable pageable);

  /** Находит вакансии по создателю, статусу и поисковому запросу */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.createdBy = :createdBy AND p.status = :status AND "
          + "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.company) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Position> findByCreatedByAndStatusAndSearch(
      @Param("createdBy") UserEntity createdBy,
      @Param("status") Position.Status status,
      @Param("search") String search,
      Pageable pageable);

  /** Находит вакансии по поисковому запросу */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE "
          + "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.company) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<Position> findBySearchTerm(@Param("search") String search, Pageable pageable);

  /** Находит вакансии по статусу и поисковому запросу */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.status = :status AND "
          + "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(p.company) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Position> findByStatusAndSearch(
      @Param("status") Position.Status status, @Param("search") String search, Pageable pageable);

  /** Находит вакансии по теме */
  @Query("SELECT p FROM Position p LEFT JOIN FETCH p.interviews JOIN p.topics t WHERE t = :topic")
  List<Position> findByTopic(@Param("topic") String topic);

  /** Находит вакансии с минимальным баллом */
  @EntityGraph(attributePaths = {"interviews"})
  List<Position> findByMinScoreGreaterThan(Double minScore);

  /** Находит вакансии, созданные после указанной даты */
  @Query("SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.createdAt >= :dateTime")
  List<Position> findByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);

  /** Находит вакансии в указанном диапазоне дат создания */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.createdAt BETWEEN :start AND :end")
  List<Position> findByCreatedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Находит вакансии по команде */
  @Query("SELECT p FROM Position p LEFT JOIN FETCH p.interviews JOIN p.team t WHERE t.id = :userId")
  List<Position> findByTeamMember(@Param("userId") String userId);

  /** Подсчитывает количество вакансий по статусу */
  long countByStatus(Position.Status status);

  /** Подсчитывает количество вакансий по компании */
  long countByCompany(String company);

  /** Подсчитывает количество вакансий по создателю */
  long countByCreatedBy(UserEntity createdBy);

  /** Находит вакансии с публичными ссылками */
  @Query(
      "SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.publicLink IS NOT NULL AND p.publicLink != ''")
  List<Position> findWithPublicLinks();

  /** Находит вакансии с публичными ссылками (альтернативный метод) */
  @Query("SELECT p FROM Position p LEFT JOIN FETCH p.interviews WHERE p.publicLink IS NOT NULL")
  List<Position> findByPublicLinkIsNotNull();

  /** Подсчитывает общее количество интервью по позиции */
  @Query("SELECT COUNT(i) FROM Interview i WHERE i.position.id = :positionId")
  long countInterviewsByPosition(@Param("positionId") Long positionId);

  /** Подсчитывает количество успешных интервью по позиции */
  @Query(
      "SELECT COUNT(i) FROM Interview i WHERE i.position.id = :positionId AND i.result = 'SUCCESSFUL'")
  long countSuccessfulInterviewsByPosition(@Param("positionId") Long positionId);

  /** Подсчитывает количество интервью в процессе по позиции */
  @Query(
      "SELECT COUNT(i) FROM Interview i WHERE i.position.id = :positionId AND i.status = 'IN_PROGRESS'")
  long countInProgressInterviewsByPosition(@Param("positionId") Long positionId);

  /** Подсчитывает количество неуспешных интервью по позиции */
  @Query(
      "SELECT COUNT(i) FROM Interview i WHERE i.position.id = :positionId AND i.result = 'UNSUCCESSFUL'")
  long countUnsuccessfulInterviewsByPosition(@Param("positionId") Long positionId);

  /** Мягкое удаление вакансии (устанавливает статус ARCHIVED) */
  @Query("UPDATE Position p SET p.status = 'ARCHIVED' WHERE p.id = :id")
  void softDelete(@Param("id") Long id);

  /** Восстановление вакансии (устанавливает статус ACTIVE) */
  @Query("UPDATE Position p SET p.status = 'ACTIVE' WHERE p.id = :id")
  void restore(@Param("id") Long id);

  /** Находит вакансию по ID с загрузкой topics в одном запросе */
  @Query(
      "SELECT DISTINCT p FROM Position p "
          + "LEFT JOIN FETCH p.topics "
          + "LEFT JOIN FETCH p.createdBy "
          + "WHERE p.id = :id")
  Optional<Position> findByIdWithTopicsAndTeam(@Param("id") Long id);
}
