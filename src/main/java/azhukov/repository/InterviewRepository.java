package azhukov.repository;

import azhukov.entity.Candidate;
import azhukov.entity.Interview;
import azhukov.entity.Position;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с собеседованиями. Расширяет BaseRepository для автоматизации общих
 * операций.
 */
@Repository
public interface InterviewRepository extends BaseRepository<Interview, Long> {

  /** Находит собеседования по кандидату */
  List<Interview> findByCandidate(Candidate candidate);

  /** Находит собеседования по кандидату с пагинацией */
  Page<Interview> findByCandidate(Candidate candidate, Pageable pageable);

  /** Находит собеседования по вакансии */
  List<Interview> findByPosition(Position position);

  /** Находит собеседования по вакансии с пагинацией */
  Page<Interview> findByPosition(Position position, Pageable pageable);

  /** Находит собеседования по статусу */
  List<Interview> findByStatus(Interview.Status status);

  /** Находит собеседования по статусу с пагинацией */
  Page<Interview> findByStatus(Interview.Status status, Pageable pageable);

  /** Находит собеседования по результату */
  List<Interview> findByResult(Interview.Result result);

  /** Находит собеседования по результату с пагинацией */
  Page<Interview> findByResult(Interview.Result result, Pageable pageable);

  /** Находит собеседования по кандидату и статусу */
  List<Interview> findByCandidateAndStatus(Candidate candidate, Interview.Status status);

  /** Находит собеседования по вакансии и статусу */
  List<Interview> findByPositionAndStatus(Position position, Interview.Status status);

  /** Находит собеседования по вакансии и результату */
  List<Interview> findByPositionAndResult(Position position, Interview.Result result);

  /** Находит активные собеседования (в процессе) */
  @Query("SELECT i FROM Interview i WHERE i.status = 'IN_PROGRESS'")
  List<Interview> findActiveInterviews();

  /** Находит завершенные собеседования */
  @Query("SELECT i FROM Interview i WHERE i.status = 'FINISHED'")
  List<Interview> findFinishedInterviews();

  /** Находит успешные собеседования */
  @Query("SELECT i FROM Interview i WHERE i.result = 'SUCCESSFUL'")
  List<Interview> findSuccessfulInterviews();

  /** Находит неуспешные собеседования */
  @Query("SELECT i FROM Interview i WHERE i.result = 'UNSUCCESSFUL'")
  List<Interview> findUnsuccessfulInterviews();

  /** Находит собеседования с высокими оценками */
  @Query("SELECT i FROM Interview i WHERE i.aiScore >= :minScore")
  List<Interview> findByMinScore(@Param("minScore") Double minScore);

  /** Находит собеседования с низкими оценками */
  @Query("SELECT i FROM Interview i WHERE i.aiScore <= :maxScore")
  List<Interview> findByMaxScore(@Param("maxScore") Double maxScore);

  /** Находит собеседования в диапазоне оценок */
  @Query("SELECT i FROM Interview i WHERE i.aiScore BETWEEN :minScore AND :maxScore")
  List<Interview> findByScoreRange(
      @Param("minScore") Double minScore, @Param("maxScore") Double maxScore);

  /** Находит собеседования, начатые после указанной даты */
  @Query("SELECT i FROM Interview i WHERE i.startedAt >= :dateTime")
  List<Interview> findByStartedAtAfter(@Param("dateTime") LocalDateTime dateTime);

  /** Находит собеседования, завершенные после указанной даты */
  @Query("SELECT i FROM Interview i WHERE i.finishedAt >= :dateTime")
  List<Interview> findByFinishedAtAfter(@Param("dateTime") LocalDateTime dateTime);

  /** Находит собеседования в диапазоне дат начала */
  @Query("SELECT i FROM Interview i WHERE i.startedAt BETWEEN :start AND :end")
  List<Interview> findByStartedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Находит собеседования в диапазоне дат завершения */
  @Query("SELECT i FROM Interview i WHERE i.finishedAt BETWEEN :start AND :end")
  List<Interview> findByFinishedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Находит собеседования по ID кандидата */
  @Query("SELECT i FROM Interview i WHERE i.candidate.id = :candidateId")
  List<Interview> findByCandidateId(@Param("candidateId") Long candidateId);

  /** Находит собеседования по ID кандидата с пагинацией */
  @Query("SELECT i FROM Interview i WHERE i.candidate.id = :candidateId")
  Page<Interview> findByCandidateId(@Param("candidateId") Long candidateId, Pageable pageable);

  /** Находит собеседования по ID вакансии */
  @Query("SELECT i FROM Interview i WHERE i.position.id = :positionId")
  List<Interview> findByPositionId(@Param("positionId") Long positionId);

  /** Находит собеседования по ID вакансии с пагинацией */
  @Query("SELECT i FROM Interview i WHERE i.position.id = :positionId")
  Page<Interview> findByPositionId(@Param("positionId") Long positionId, Pageable pageable);

  /** Подсчитывает количество собеседований по статусу */
  long countByStatus(Interview.Status status);

  /** Подсчитывает количество собеседований по результату */
  long countByResult(Interview.Result result);

  /** Подсчитывает количество собеседований по кандидату */
  long countByCandidate(Candidate candidate);

  /** Подсчитывает количество собеседований по вакансии */
  long countByPosition(Position position);

  /** Подсчитывает количество успешных собеседований */
  @Query("SELECT COUNT(i) FROM Interview i WHERE i.result = 'SUCCESSFUL'")
  long countSuccessful();

  /** Подсчитывает количество неуспешных собеседований */
  @Query("SELECT COUNT(i) FROM Interview i WHERE i.result = 'UNSUCCESSFUL'")
  long countUnsuccessful();

  /** Находит среднюю оценку по всем собеседованиям */
  @Query("SELECT AVG(i.aiScore) FROM Interview i WHERE i.aiScore IS NOT NULL")
  Double findAverageScore();

  /** Находит среднюю оценку по вакансии */
  @Query(
      "SELECT AVG(i.aiScore) FROM Interview i WHERE i.position = :position AND i.aiScore IS NOT NULL")
  Double findAverageScoreByPosition(@Param("position") Position position);

  /** Мягкое удаление собеседования (устанавливает статус FINISHED и результат UNSUCCESSFUL) */
  @Query("UPDATE Interview i SET i.status = 'FINISHED', i.result = 'UNSUCCESSFUL' WHERE i.id = :id")
  void softDelete(@Param("id") Long id);

  /** Восстановление собеседования (устанавливает статус NOT_STARTED и убирает результат) */
  @Query("UPDATE Interview i SET i.status = 'NOT_STARTED', i.result = NULL WHERE i.id = :id")
  void restore(@Param("id") Long id);
}
