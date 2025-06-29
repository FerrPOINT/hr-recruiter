package azhukov.config;

import azhukov.service.ai.AIService;
import azhukov.service.ai.claude.ClaudeService;
import azhukov.service.ai.openrouter.OpenRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация для AI сервисов. OpenRouter используется как основной сервис.
 *
 * @author AI Team
 * @version 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIServiceConfig {

  private final ClaudeConfig claudeConfig;
  private final OpenRouterConfig openRouterConfig;
  private final RestTemplate restTemplate;

  /** Основной AI сервис - OpenRouter. */
  @Bean
  @Primary
  public AIService openRouterService() {
    log.info("Инициализация OpenRouter AI сервиса");
    return new OpenRouterService(openRouterConfig, restTemplate);
  }

  /**
   * Альтернативный AI сервис - Claude. Используется только если явно указано в настройках. В данный
   * момент отключен по умолчанию.
   */
  @Bean
  @ConditionalOnProperty(name = "ai.service.type", havingValue = "claude")
  public AIService claudeService() {
    log.info("Инициализация Claude AI сервиса");
    return new ClaudeService(claudeConfig, restTemplate);
  }
}
