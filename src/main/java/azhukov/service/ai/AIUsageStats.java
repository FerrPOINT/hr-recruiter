package azhukov.service.ai;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Статистика использования AI сервиса. Следует принципу Single Responsibility (SOLID) - отвечает
 * только за хранение статистики.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Builder
public class AIUsageStats {

  /** Название AI сервиса */
  private String serviceName;

  /** Общее количество запросов */
  private long totalRequests;

  /** Количество успешных запросов */
  private long successfulRequests;

  /** Количество неуспешных запросов */
  private long failedRequests;

  /** Общее количество токенов */
  private long totalTokens;

  /** Среднее время ответа в миллисекундах */
  private double averageResponseTimeMs;

  /** Время последнего запроса */
  private LocalDateTime lastRequestTime;

  /** Время первого запроса */
  private LocalDateTime firstRequestTime;

  /** Процент успешных запросов */
  public double getSuccessRate() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) successfulRequests / totalRequests * 100.0;
  }

  /** Процент неуспешных запросов */
  public double getFailureRate() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) failedRequests / totalRequests * 100.0;
  }

  /** Среднее количество токенов на запрос */
  public double getAverageTokensPerRequest() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) totalTokens / totalRequests;
  }

  /** Проверяет, есть ли статистика */
  public boolean hasData() {
    return totalRequests > 0;
  }

  /** Создает копию статистики */
  public AIUsageStats copy() {
    return AIUsageStats.builder()
        .serviceName(this.serviceName)
        .totalRequests(this.totalRequests)
        .successfulRequests(this.successfulRequests)
        .failedRequests(this.failedRequests)
        .totalTokens(this.totalTokens)
        .averageResponseTimeMs(this.averageResponseTimeMs)
        .lastRequestTime(this.lastRequestTime)
        .firstRequestTime(this.firstRequestTime)
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        "AIUsageStats{service='%s', totalRequests=%d, successRate=%.2f%%, avgResponseTime=%.2fms}",
        serviceName, totalRequests, getSuccessRate(), averageResponseTimeMs);
  }
}
