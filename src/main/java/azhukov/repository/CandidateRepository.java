package azhukov.repository;

import azhukov.entity.Candidate;
import azhukov.entity.Position;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с кандидатами. Расширяет BaseRepository для автоматизации общих операций.
 */
@Repository
public interface CandidateRepository extends BaseRepository<Candidate, Long> {

  /** Находит кандидатов по вакансии */
  List<Candidate> findByPosition(Position position);

  /** Находит кандидатов по вакансии с пагинацией */
  Page<Candidate> findByPosition(Position position, Pageable pageable);

  /** Находит кандидатов по статусу */
  List<Candidate> findByStatus(Candidate.Status status);

  /** Находит кандидатов по статусу с пагинацией */
  Page<Candidate> findByStatus(Candidate.Status status, Pageable pageable);

  /** Находит кандидатов по email */
  Candidate findByEmail(String email);

  /** Находит всех кандидатов по email (для обработки дубликатов) */
  List<Candidate> findAllByEmail(String email);

  /** Проверяет существование кандидата по email */
  boolean existsByEmail(String email);

  /** Находит кандидатов по поисковому запросу */
  @Query(
      "SELECT c FROM Candidate c WHERE "
          + "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<Candidate> findBySearchTerm(@Param("search") String search, Pageable pageable);

  /** Находит кандидатов по статусу и поисковому запросу */
  @Query(
      "SELECT c FROM Candidate c WHERE c.status = :status AND "
          + "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Candidate> findByStatusAndSearch(
      @Param("status") Candidate.Status status, @Param("search") String search, Pageable pageable);

  /** Находит кандидатов по навыкам */
  @Query("SELECT c FROM Candidate c JOIN c.skills s WHERE s = :skill")
  List<Candidate> findBySkill(@Param("skill") String skill);

  /** Находит кандидатов по нескольким навыкам */
  @Query("SELECT c FROM Candidate c JOIN c.skills s WHERE s IN :skills")
  List<Candidate> findBySkills(@Param("skills") List<String> skills);

  /** Находит кандидатов по опыту работы */
  List<Candidate> findByExperienceYearsGreaterThanEqual(Integer experienceYears);

  /** Находит кандидатов, созданных после указанной даты */
  @Query("SELECT c FROM Candidate c WHERE c.createdAt >= :dateTime")
  List<Candidate> findByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);

  /** Находит кандидатов в указанном диапазоне дат создания */
  @Query("SELECT c FROM Candidate c WHERE c.createdAt BETWEEN :start AND :end")
  List<Candidate> findByCreatedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Находит кандидатов по вакансии и статусу */
  List<Candidate> findByPositionAndStatus(Position position, Candidate.Status status);

  /** Находит кандидатов с резюме */
  @Query("SELECT c FROM Candidate c WHERE c.resumeUrl IS NOT NULL AND c.resumeUrl != ''")
  List<Candidate> findWithResume();

  /** Находит кандидатов с сопроводительными письмами */
  @Query("SELECT c FROM Candidate c WHERE c.coverLetter IS NOT NULL AND c.coverLetter != ''")
  List<Candidate> findWithCoverLetter();

  /** Подсчитывает количество кандидатов по статусу */
  long countByStatus(Candidate.Status status);

  /** Подсчитывает количество кандидатов по вакансии */
  long countByPosition(Position position);

  /** Подсчитывает количество кандидатов по вакансии и статусу */
  long countByPositionAndStatus(Position position, Candidate.Status status);

  /** Находит кандидатов по ID вакансии */
  @Query("SELECT c FROM Candidate c WHERE c.position.id = :positionId")
  List<Candidate> findByPositionId(@Param("positionId") Long positionId);

  /** Находит кандидатов по ID вакансии с пагинацией */
  @Query("SELECT c FROM Candidate c WHERE c.position.id = :positionId")
  Page<Candidate> findByPositionId(@Param("positionId") Long positionId, Pageable pageable);

  /** Мягкое удаление кандидата (устанавливает статус REJECTED) */
  @Query("UPDATE Candidate c SET c.status = 'REJECTED' WHERE c.id = :id")
  void softDelete(@Param("id") Long id);

  /** Восстановление кандидата (устанавливает статус NEW) */
  @Query("UPDATE Candidate c SET c.status = 'NEW' WHERE c.id = :id")
  void restore(@Param("id") Long id);

  /** Находит кандидата по телефону */
  Candidate findByPhone(String phone);

  /** Находит всех кандидатов по телефону (для обработки дубликатов) */
  List<Candidate> findAllByPhone(String phone);

  /** Проверяет существование кандидата по телефону */
  boolean existsByPhone(String phone);

  // === МЕТОДЫ ДЛЯ ВИДЖЕТОВ ===

  /** Подсчитывает количество кандидатов по источнику */
  long countBySource(String source);

  /** Подсчитывает количество кандидатов по статусу и источнику */
  long countByStatusAndSource(Candidate.Status status, String source);

  /** Получить группировку кандидатов по источникам */
  @Query("SELECT c.source, COUNT(c) FROM Candidate c GROUP BY c.source")
  List<Object[]> countBySourceGrouped();

  /** Находит кандидатов по источнику */
  List<Candidate> findBySource(String source);

  /** Находит кандидатов по источнику с пагинацией */
  Page<Candidate> findBySource(String source, Pageable pageable);
}
