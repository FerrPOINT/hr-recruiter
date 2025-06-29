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
  private String apiKey = "${ANTHROPIC_API_KEY:}";

  /** URL API Claude */
  private String apiUrl = "https://api.anthropic.com/v1/messages";

  /** Модель Claude по умолчанию */
  private String model = "claude-3-sonnet-20240229";

  /** Максимальное количество токенов по умолчанию */
  private Integer maxTokens = ApplicationProperties.Constants.DEFAULT_MAX_TOKENS;

  /** Температура генерации по умолчанию (0.0 - 1.0) */
  private Double temperature = ApplicationProperties.Constants.DEFAULT_TEMPERATURE;

  /** Таймаут запроса в миллисекундах */
  private Integer timeout = ApplicationProperties.Constants.DEFAULT_TIMEOUT_MS;

  /** Максимальное количество повторных попыток */
  private Integer maxRetries = ApplicationProperties.Constants.DEFAULT_MAX_RETRIES;

  /** Задержка между повторными попытками в миллисекундах */
  private Integer retryDelay = ApplicationProperties.Constants.DEFAULT_RETRY_DELAY_MS;

  /** Включить кэширование промптов */
  private Boolean enablePromptCaching = true;

  /** Размер кэша промптов */
  private Integer promptCacheSize = ApplicationProperties.Constants.DEFAULT_CACHE_SIZE;

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
    return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
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
