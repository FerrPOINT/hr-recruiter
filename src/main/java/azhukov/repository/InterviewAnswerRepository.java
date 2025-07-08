package azhukov.repository;

import azhukov.entity.InterviewAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с ответами на вопросы собеседований. Предоставляет методы для поиска и
 * управления ответами кандидатов.
 */
@Repository
public interface InterviewAnswerRepository extends BaseRepository<InterviewAnswer, Long> {

  /**
   * Находит все ответы для конкретного собеседования
   *
   * @param interviewId ID собеседования
   * @return список ответов
   */
  List<InterviewAnswer> findByInterviewId(Long interviewId);

  /**
   * Находит все ответы для конкретного вопроса
   *
   * @param questionId ID вопроса
   * @return список ответов
   */
  List<InterviewAnswer> findByQuestionId(Long questionId);

  /**
   * Находит ответы с оценкой выше указанной
   *
   * @param minScore минимальная оценка
   * @return список ответов
   */
  List<InterviewAnswer> findByScoreGreaterThan(Double minScore);

  /**
   * Находит ответы с оценкой ниже указанной
   *
   * @param maxScore максимальная оценка
   * @return список ответов
   */
  List<InterviewAnswer> findByScoreLessThan(Double maxScore);

  /**
   * Находит ответы по типу (текст, аудио, видео)
   *
   * @param answerType тип ответа
   * @return список ответов
   */
  @Query(
      "SELECT ia FROM InterviewAnswer ia WHERE "
          + "(:answerType = 'TEXT' AND ia.answerText IS NOT NULL) OR "
          + "(:answerType = 'AUDIO' AND ia.audioUrl IS NOT NULL) OR "
          + "(:answerType = 'VIDEO' AND ia.videoUrl IS NOT NULL)")
  List<InterviewAnswer> findByAnswerType(@Param("answerType") String answerType);

  /**
   * Находит ответы с транскрибацией
   *
   * @return список ответов с транскрибацией
   */
  @Query(
      "SELECT ia FROM InterviewAnswer ia WHERE ia.formattedTranscription IS NOT NULL OR ia.rawTranscription IS NOT NULL")
  List<InterviewAnswer> findWithTranscription();

  /**
   * Подсчитывает количество ответов для собеседования
   *
   * @param interviewId ID собеседования
   * @return количество ответов
   */
  long countByInterviewId(Long interviewId);

  /**
   * Находит среднюю оценку для собеседования
   *
   * @param interviewId ID собеседования
   * @return средняя оценка
   */
  @Query(
      "SELECT AVG(ia.score) FROM InterviewAnswer ia WHERE ia.interview.id = :interviewId AND ia.score IS NOT NULL")
  Double findAverageScoreByInterviewId(@Param("interviewId") Long interviewId);

  /**
   * Проверяет, существует ли ответ на конкретный вопрос в собеседовании
   *
   * @param interviewId ID собеседования
   * @param questionId ID вопроса
   * @return true если ответ существует
   */
  boolean existsByInterviewIdAndQuestionId(Long interviewId, Long questionId);
}
