package azhukov.config;

import azhukov.service.ai.AIService;
import azhukov.service.ai.openrouter.OpenRouterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final OpenRouterConfig openRouterConfig;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  /** Основной AI сервис - OpenRouter. */
  @Bean
  @Primary
  public AIService openRouterService() {
    log.info("Инициализация OpenRouter AI сервиса");
    return new OpenRouterService(openRouterConfig, restTemplate, objectMapper);
  }
}
