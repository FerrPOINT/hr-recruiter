package azhukov.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для OpenRouter API. OpenRouter предоставляет унифицированный интерфейс для множества
 * LLM моделей.
 *
 * @author AI Team
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterConfig {

  /** API ключ для OpenRouter */
  private String apiKey = "${OPENROUTER_API_KEY:}";

  /** URL API OpenRouter */
  private String apiUrl = "https://openrouter.ai/api/v1/chat/completions";

  /** Модель по умолчанию */
  private String model = "anthropic/claude-3.5-sonnet";

  /** Максимальное количество токенов по умолчанию */
  private Integer maxTokens = ApplicationProperties.Constants.DEFAULT_MAX_TOKENS;

  /** Температура генерации по умолчанию (0.0 - 2.0) */
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
        "Content-Type: application/json, Authorization: Bearer %s, HTTP-Referer: %s, X-Title: %s",
        isApiKeyConfigured() ? "configured" : "not-configured",
        "https://github.com/azhukov/hr-recruiter-back",
        "HR Recruiter Backend");
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
