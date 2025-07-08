package azhukov.controller;

import azhukov.api.AnalyticsReportsApi;
import azhukov.entity.Candidate;
import azhukov.entity.Interview;
import azhukov.entity.Position;
import azhukov.model.CandidateStats;
import azhukov.model.InterviewStats;
import azhukov.model.MonthlyReport;
import azhukov.model.PositionStats;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для аналитики и отчетов. Реализует интерфейс AnalyticsReportsApi, сгенерированный из
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsReportsController extends BaseController implements AnalyticsReportsApi {

  private final CandidateRepository candidateRepository;
  private final InterviewRepository interviewRepository;
  private final PositionRepository positionRepository;

  @Override
  public ResponseEntity<CandidateStats> getCandidatesStats() {
    log.info("Getting candidates statistics");

    // Оптимизированные запросы для получения статистики кандидатов
    long total = candidateRepository.count();
    long inProgress = candidateRepository.countByStatus(Candidate.Status.IN_PROGRESS);
    long finished = candidateRepository.countByStatus(Candidate.Status.FINISHED);
    long hired = candidateRepository.countByStatus(Candidate.Status.HIRED);

    CandidateStats stats = new CandidateStats();
    stats.setTotal(total);
    stats.setInProgress(inProgress);
    stats.setFinished(finished);
    stats.setHired(hired);

    return ResponseEntity.ok(stats);
  }

  @Override
  public ResponseEntity<InterviewStats> getInterviewsStats() {
    log.info("Getting interviews statistics");

    // Оптимизированные запросы для получения статистики интервью
    long total = interviewRepository.count();
    long successful = interviewRepository.countSuccessful();
    long unsuccessful = interviewRepository.countUnsuccessful();

    InterviewStats stats = new InterviewStats();
    stats.setTotal(total);
    stats.setSuccessful(successful);
    stats.setUnsuccessful(unsuccessful);

    return ResponseEntity.ok(stats);
  }

  @Override
  public ResponseEntity<List<PositionStats>> getPositionsStats() {
    log.info("Getting positions statistics");

    // TODO: Оптимизировать - использовать один запрос с JOIN вместо N+1
    List<Position> positions = positionRepository.findByStatus(Position.Status.ACTIVE);

    List<PositionStats> statsList = new ArrayList<>();

    for (Position position : positions) {
      // Оптимизированные запросы для каждой вакансии
      long interviewsTotal = positionRepository.countInterviewsByPosition(position.getId());
      long interviewsSuccessful =
          positionRepository.countSuccessfulInterviewsByPosition(position.getId());
      long interviewsUnsuccessful =
          positionRepository.countUnsuccessfulInterviewsByPosition(position.getId());
      long interviewsInProgress =
          positionRepository.countInProgressInterviewsByPosition(position.getId());

      PositionStats stats = new PositionStats();
      stats.setPositionId(position.getId());
      stats.setInterviewsTotal(interviewsTotal);
      stats.setInterviewsSuccessful(interviewsSuccessful);
      stats.setInterviewsUnsuccessful(interviewsUnsuccessful);
      stats.setInterviewsInProgress(interviewsInProgress);

      statsList.add(stats);
    }

    return ResponseEntity.ok(statsList);
  }

  @Override
  public ResponseEntity<List<MonthlyReport>> getReports() {
    log.info("Getting monthly reports");

    // Получаем данные за последние 12 месяцев
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startDate =
        now.minusMonths(11).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

    List<MonthlyReport> reports = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("ru"));

    for (int i = 0; i < 12; i++) {
      LocalDateTime monthStart = startDate.plusMonths(i);
      LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

      // TODO: Оптимизировать - использовать агрегатные запросы вместо загрузки всех данных
      List<Interview> monthInterviews =
          interviewRepository.findByStartedAtBetween(monthStart, monthEnd);

      long totalInterviews = monthInterviews.size();
      long successful =
          monthInterviews.stream()
              .filter(interview -> Interview.Result.SUCCESSFUL.equals(interview.getResult()))
              .count();
      long unsuccessful =
          monthInterviews.stream()
              .filter(interview -> Interview.Result.UNSUCCESSFUL.equals(interview.getResult()))
              .count();

      // Вычисляем средний балл
      double avgScore =
          monthInterviews.stream()
              .filter(interview -> interview.getAiScore() != null)
              .mapToDouble(Interview::getAiScore)
              .average()
              .orElse(0.0);

      // Вычисляем динамику (рост/падение по сравнению с предыдущим месяцем)
      long dynamics = 0;
      if (i > 0) {
        LocalDateTime prevMonthStart = monthStart.minusMonths(1);
        LocalDateTime prevMonthEnd = monthStart.minusSeconds(1);
        List<Interview> prevMonthInterviews =
            interviewRepository.findByStartedAtBetween(prevMonthStart, prevMonthEnd);
        long prevTotal = prevMonthInterviews.size();
        dynamics = prevTotal > 0 ? ((totalInterviews - prevTotal) * 100) / prevTotal : 0;
      }

      MonthlyReport report = new MonthlyReport();
      report.setMonth(monthStart.format(formatter));
      report.setTotalInterviews(totalInterviews);
      report.setSuccessful(successful);
      report.setUnsuccessful(unsuccessful);
      report.setAvgScore((float) avgScore);
      report.setDynamics(dynamics);

      reports.add(report);
    }

    return ResponseEntity.ok(reports);
  }
}
