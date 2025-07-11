package azhukov.service;

import azhukov.entity.ActivityLog;
import azhukov.model.ActivityItem;
import azhukov.model.PaginatedResponse;
import azhukov.repository.ActivityLogRepository;
import azhukov.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Сервис для работы с лентой активности пользователей */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

  private final ActivityLogRepository activityLogRepository;
  private final UserRepository userRepository;

  /** Получить ленту активности с пагинацией */
  public PaginatedResponse getActivityFeed(
      Optional<Long> limit, Optional<String> type, Optional<Long> page) {

    // Параметры по умолчанию
    int pageSize = limit.map(Long::intValue).orElse(20);
    int pageNumber = page.map(Long::intValue).orElse(0);
    ActivityLog.ActivityType activityType = type.map(this::parseActivityType).orElse(null);

    // Создаем Pageable
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    // Получаем данные
    Page<ActivityLog> activityPage =
        activityLogRepository.findRecentActivity(activityType, pageable);

    // Преобразуем в DTO
    List<ActivityItem> activityItems =
        activityPage.getContent().stream()
            .map(this::convertToActivityItem)
            .collect(Collectors.toList());

    // Создаем ответ
    PaginatedResponse response = new PaginatedResponse();
    response.setContent(
        activityItems.stream().map(item -> (Object) item).collect(Collectors.toList()));
    response.setTotalElements(activityPage.getTotalElements());
    response.setTotalPages((long) activityPage.getTotalPages());
    response.setNumber((long) activityPage.getNumber());
    response.setSize((long) activityPage.getSize());

    log.info(
        "Retrieved {} activity items, page {} of {}",
        activityItems.size(),
        pageNumber,
        activityPage.getTotalPages());

    return response;
  }

  /** Логировать активность */
  public void logActivity(
      ActivityLog.ActivityType activityType,
      String title,
      String description,
      Long userId,
      Long entityId,
      ActivityLog.EntityType entityType,
      Map<String, Object> metadata) {

    ActivityLog activityLog =
        ActivityLog.builder()
            .activityType(activityType)
            .title(title)
            .description(description)
            .userId(userId)
            .entityId(entityId)
            .entityType(entityType)
            .metadata(metadata != null ? metadata.toString() : null)
            .build();

    activityLogRepository.save(activityLog);
    log.info("Logged activity: {} - {}", activityType, title);
  }

  /** Логировать активность (упрощенная версия) */
  public void logActivity(
      ActivityLog.ActivityType activityType,
      String title,
      Long userId,
      Long entityId,
      ActivityLog.EntityType entityType) {

    logActivity(activityType, title, null, userId, entityId, entityType, null);
  }

  /** Получить активность по пользователю */
  public List<ActivityItem> getUserActivity(Long userId, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    Page<ActivityLog> activityPage = activityLogRepository.findByUserId(userId, pageable);

    return activityPage.getContent().stream()
        .map(this::convertToActivityItem)
        .collect(Collectors.toList());
  }

  /** Получить активность по сущности */
  public List<ActivityItem> getEntityActivity(ActivityLog.EntityType entityType, Long entityId) {
    List<ActivityLog> activities = activityLogRepository.findByEntity(entityType, entityId);

    return activities.stream().map(this::convertToActivityItem).collect(Collectors.toList());
  }

  /** Получить статистику активности */
  public Map<ActivityLog.ActivityType, Long> getActivityStats() {
    Map<ActivityLog.ActivityType, Long> stats =
        Map.of(
            ActivityLog.ActivityType.INTERVIEW,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.INTERVIEW),
            ActivityLog.ActivityType.POSITION,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.POSITION),
            ActivityLog.ActivityType.CANDIDATE,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.CANDIDATE),
            ActivityLog.ActivityType.HIRED,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.HIRED),
            ActivityLog.ActivityType.REPORT,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.REPORT),
            ActivityLog.ActivityType.LOGIN,
                activityLogRepository.countByActivityType(ActivityLog.ActivityType.LOGIN));

    return stats;
  }

  /** Получить активность за последние N дней */
  public List<ActivityItem> getRecentActivity(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    List<ActivityLog> activities = activityLogRepository.findActivitySince(since);

    return activities.stream().map(this::convertToActivityItem).collect(Collectors.toList());
  }

  /** Преобразовать ActivityLog в ActivityItem */
  private ActivityItem convertToActivityItem(ActivityLog activityLog) {
    ActivityItem item = new ActivityItem();
    item.setId(activityLog.getId());
    item.setType(
        ActivityItem.TypeEnum.fromValue(activityLog.getActivityType().name().toLowerCase()));
    item.setTitle(activityLog.getTitle());
    item.setDescription(activityLog.getDescription());
    item.setUserId(activityLog.getUserId());
    item.setEntityId(activityLog.getEntityId());

    if (activityLog.getEntityType() != null) {
      item.setEntityType(
          ActivityItem.EntityTypeEnum.fromValue(activityLog.getEntityType().name().toLowerCase()));
    }

    // Конвертируем LocalDateTime в OffsetDateTime
    if (activityLog.getCreatedAt() != null) {
      item.setCreatedAt(activityLog.getCreatedAt().atOffset(ZoneOffset.UTC));
    }

    // Получаем имя пользователя
    if (activityLog.getUserId() != null) {
      userRepository
          .findById(activityLog.getUserId())
          .ifPresent(user -> item.setUserName(user.getFirstName() + " " + user.getLastName()));
    }

    return item;
  }

  /** Парсить тип активности из строки */
  private ActivityLog.ActivityType parseActivityType(String type) {
    try {
      return ActivityLog.ActivityType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid activity type: {}", type);
      return null;
    }
  }
}
