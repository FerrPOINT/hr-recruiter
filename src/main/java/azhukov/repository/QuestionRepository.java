package azhukov.repository;

import azhukov.entity.Position;
import azhukov.entity.Question;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с вопросами. Расширяет BaseRepository для автоматизации общих операций.
 */
@Repository
public interface QuestionRepository extends BaseRepository<Question, String> {

  /** Находит вопросы по вакансии */
  List<Question> findByPosition(Position position);

  /** Находит вопросы по вакансии с пагинацией */
  Page<Question> findByPosition(Position position, Pageable pageable);

  /** Находит вопросы по типу */
  List<Question> findByType(Question.Type type);

  /** Находит вопросы по типу с пагинацией */
  Page<Question> findByType(Question.Type type, Pageable pageable);

  /** Находит вопросы по вакансии и типу */
  List<Question> findByPositionAndType(Position position, Question.Type type);

  /** Находит обязательные вопросы */
  List<Question> findByRequiredTrue();

  /** Находит обязательные вопросы по вакансии */
  List<Question> findByPositionAndRequiredTrue(Position position);

  /** Находит вопросы по поисковому запросу */
  @Query("SELECT q FROM Question q WHERE LOWER(q.text) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<Question> findBySearchTerm(@Param("search") String search, Pageable pageable);

  /** Находит вопросы по вакансии и поисковому запросу */
  @Query(
      "SELECT q FROM Question q WHERE q.position = :position AND "
          + "LOWER(q.text) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<Question> findByPositionAndSearch(
      @Param("position") Position position, @Param("search") String search, Pageable pageable);

  /** Находит вопросы по порядку */
  List<Question> findByPositionOrderByOrderAsc(Position position);

  /** Находит вопросы с максимальной длительностью */
  List<Question> findByMaxDurationGreaterThan(Integer maxDuration);

  /** Находит вопросы с вариантами ответов (тип CHOICE) */
  @Query("SELECT q FROM Question q WHERE q.type = 'CHOICE' AND SIZE(q.options) > 0")
  List<Question> findChoiceQuestionsWithOptions();

  /** Находит вопросы по вакансии с вариантами ответов */
  @Query(
      "SELECT q FROM Question q WHERE q.position = :position AND q.type = 'CHOICE' AND SIZE(q.options) > 0")
  List<Question> findChoiceQuestionsWithOptionsByPosition(@Param("position") Position position);

  /** Находит вопросы, созданные после указанной даты */
  @Query("SELECT q FROM Question q WHERE q.createdAt >= :dateTime")
  List<Question> findByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);

  /** Находит вопросы в указанном диапазоне дат создания */
  @Query("SELECT q FROM Question q WHERE q.createdAt BETWEEN :start AND :end")
  List<Question> findByCreatedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Находит вопросы по ID вакансии */
  @Query("SELECT q FROM Question q WHERE q.position.id = :positionId")
  List<Question> findByPositionId(@Param("positionId") String positionId);

  /** Находит вопросы по ID вакансии с пагинацией */
  @Query("SELECT q FROM Question q WHERE q.position.id = :positionId")
  Page<Question> findByPositionId(@Param("positionId") String positionId, Pageable pageable);

  /** Находит вопросы по ID вакансии, отсортированные по порядку */
  @Query("SELECT q FROM Question q WHERE q.position.id = :positionId ORDER BY q.order ASC")
  List<Question> findByPositionIdOrderByOrderAsc(@Param("positionId") String positionId);

  /** Подсчитывает количество вопросов по вакансии */
  long countByPosition(Position position);

  /** Подсчитывает количество вопросов по типу */
  long countByType(Question.Type type);

  /** Подсчитывает количество обязательных вопросов по вакансии */
  long countByPositionAndRequiredTrue(Position position);

  /** Находит максимальный порядок вопросов для вакансии */
  @Query("SELECT MAX(q.order) FROM Question q WHERE q.position = :position")
  Integer findMaxOrderByPosition(@Param("position") Position position);

  /** Мягкое удаление вопроса (устанавливает required = false) */
  @Query("UPDATE Question q SET q.required = false WHERE q.id = :id")
  void softDelete(@Param("id") String id);

  /** Восстановление вопроса (устанавливает required = true) */
  @Query("UPDATE Question q SET q.required = true WHERE q.id = :id")
  void restore(@Param("id") String id);
}
