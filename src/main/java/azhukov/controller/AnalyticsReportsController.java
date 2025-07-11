package azhukov.controller;

import azhukov.api.AnalyticsReportsApi;
import azhukov.entity.Candidate;
import azhukov.entity.Interview;
import azhukov.entity.Position;
import azhukov.model.CandidateStats;
import azhukov.model.CandidateStatsBySource;
import azhukov.model.InterviewStats;
import azhukov.model.MonthlyReport;
import azhukov.model.PaginatedResponse;
import azhukov.model.PositionStats;
import azhukov.model.PositionStatsByLevel;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import azhukov.service.ActivityService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
  private final ActivityService activityService;

  @Override
  public ResponseEntity<PaginatedResponse> getActivityFeed(
      Optional<Long> limit, Optional<String> type, Optional<Long> page) {
    log.info("Getting activity feed with limit: {}, type: {}, page: {}", limit, type, page);

    PaginatedResponse response = activityService.getActivityFeed(limit, type, page);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CandidateStats> getCandidatesStats() {
    log.info("Getting candidates statistics");

    // Оптимизированные запросы для получения статистики кандидатов
    long total = candidateRepository.count();
    long inProgress = candidateRepository.countByStatus(Candidate.Status.IN_PROGRESS);
    long finished = candidateRepository.countByStatus(Candidate.Status.FINISHED);
    long hired = candidateRepository.countByStatus(Candidate.Status.HIRED);
    long rejected = candidateRepository.countByStatus(Candidate.Status.REJECTED);

    CandidateStats stats = new CandidateStats();
    stats.setTotal(total);
    stats.setInProgress(inProgress);
    stats.setFinished(finished);
    stats.setHired(hired);
    stats.setRejected(rejected);

    // Группировка по источникам
    CandidateStatsBySource bySource = new CandidateStatsBySource();

    // Используем более безопасный подход - получаем все записи и группируем в памяти
    List<Candidate> allCandidates = candidateRepository.findAll();

    long directCount =
        allCandidates.stream()
            .filter(c -> c.getSource() != null && "direct".equals(c.getSource()))
            .count();
    long referralCount =
        allCandidates.stream()
            .filter(c -> c.getSource() != null && "referral".equals(c.getSource()))
            .count();
    long jobBoardCount =
        allCandidates.stream()
            .filter(c -> c.getSource() != null && "jobBoard".equals(c.getSource()))
            .count();
    long socialCount =
        allCandidates.stream()
            .filter(c -> c.getSource() != null && "social".equals(c.getSource()))
            .count();

    log.info(
        "Source counts - direct: {}, referral: {}, jobBoard: {}, social: {}",
        directCount,
        referralCount,
        jobBoardCount,
        socialCount);

    bySource.setDirect(directCount);
    bySource.setReferral(referralCount);
    bySource.setJobBoard(jobBoardCount);
    bySource.setSocial(socialCount);

    stats.setBySource(bySource);

    return ResponseEntity.ok(stats);
  }

  @Override
  public ResponseEntity<InterviewStats> getInterviewsStats() {
    log.info("Getting interviews statistics");

    // Оптимизированные запросы для получения статистики интервью
    long total = interviewRepository.count();
    long successful = interviewRepository.countSuccessful();
    long unsuccessful = interviewRepository.countUnsuccessful();
    long inProgress = interviewRepository.countByStatus(Interview.Status.IN_PROGRESS);
    long notStarted = interviewRepository.countByStatus(Interview.Status.NOT_STARTED);
    long cancelled =
        interviewRepository.countByStatus(Interview.Status.FINISHED) - successful - unsuccessful;

    InterviewStats stats = new InterviewStats();
    stats.setTotal(total);
    stats.setSuccessful(successful);
    stats.setUnsuccessful(unsuccessful);
    stats.setInProgress(inProgress);
    stats.setNotStarted(notStarted);
    stats.setCancelled(cancelled);

    return ResponseEntity.ok(stats);
  }

  @Override
  public ResponseEntity<PositionStats> getPositionsStats(Optional<Boolean> includeDetails) {
    log.info("Getting positions statistics with includeDetails: {}", includeDetails);

    // Получаем общую статистику по вакансиям
    long total = positionRepository.count();
    long active = positionRepository.countByStatus(Position.Status.ACTIVE);
    long paused = positionRepository.countByStatus(Position.Status.PAUSED);
    long archived = positionRepository.countByStatus(Position.Status.ARCHIVED);

    // Получаем статистику по интервью
    long interviewsTotal = interviewRepository.count();
    long interviewsSuccessful = interviewRepository.countSuccessful();
    long interviewsUnsuccessful = interviewRepository.countUnsuccessful();
    long interviewsInProgress = interviewRepository.countByStatus(Interview.Status.IN_PROGRESS);

    PositionStats stats = new PositionStats();
    stats.setTotal(total);
    stats.setActive(active);
    stats.setPaused(paused);
    stats.setArchived(archived);
    stats.setInterviewsTotal(interviewsTotal);
    stats.setInterviewsSuccessful(interviewsSuccessful);
    stats.setInterviewsUnsuccessful(interviewsUnsuccessful);
    stats.setInterviewsInProgress(interviewsInProgress);

    // Группировка по уровням если includeDetails=true
    if (includeDetails.orElse(false)) {
      PositionStatsByLevel byLevel = new PositionStatsByLevel();
      byLevel.setJunior(positionRepository.countByLevel(Position.Level.JUNIOR));
      byLevel.setMiddle(positionRepository.countByLevel(Position.Level.MIDDLE));
      byLevel.setSenior(positionRepository.countByLevel(Position.Level.SENIOR));
      byLevel.setLead(positionRepository.countByLevel(Position.Level.LEAD));
      stats.setByLevel(byLevel);
    }

    return ResponseEntity.ok(stats);
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
