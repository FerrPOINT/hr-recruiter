package azhukov.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для Claude AI API. Следует принципу Single Responsibility (SOLID) - отвечает только
 * за настройки Claude.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "anthropic")
public class ClaudeConfig {

  /** API ключ для Claude */
  private String apiKey = "sk-demo-key";

  /** URL API Claude */
  private String apiUrl = "https://api.anthropic.com/v1/messages";

  /** Модель Claude по умолчанию */
  private String model = "claude-3-sonnet-20240229";

  /** Максимальное количество токенов по умолчанию */
  private Integer maxTokens = 1000;

  /** Температура генерации по умолчанию (0.0 - 1.0) */
  private Double temperature = 0.7;

  /** Таймаут запроса в миллисекундах */
  private Integer timeout = 30000;

  /** Максимальное количество повторных попыток */
  private Integer maxRetries = 3;

  /** Задержка между повторными попытками в миллисекундах */
  private Integer retryDelay = 1000;

  /** Включить кэширование промптов */
  private Boolean enablePromptCaching = true;

  /** Размер кэша промптов */
  private Integer promptCacheSize = 1000;

  /** Включить логирование запросов */
  private Boolean enableRequestLogging = true;

  /** Включить метрики использования */
  private Boolean enableUsageMetrics = true;

  /**
   * Проверяет, настроен ли API ключ.
   *
   * @return true если ключ настроен
   */
  public boolean isApiKeyConfigured() {
    return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk-demo-key");
  }

  /**
   * Получает полный URL для API запросов.
   *
   * @return Полный URL
   */
  public String getFullApiUrl() {
    return apiUrl;
  }

  /**
   * Получает заголовки для API запросов.
   *
   * @return Заголовки в виде строки
   */
  public String getHeadersInfo() {
    return String.format(
        "Content-Type: application/json, x-api-key: %s, anthropic-version: 2023-06-01",
        isApiKeyConfigured() ? "configured" : "not-configured");
  }

  /**
   * Проверяет, включены ли расширенные функции.
   *
   * @return true если расширенные функции включены
   */
  public boolean isAdvancedFeaturesEnabled() {
    return enablePromptCaching && enableUsageMetrics;
  }
}
