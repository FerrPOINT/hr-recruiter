package azhukov.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Конфигурация для отключения AI сервисов в тестах */
@Configuration
@Profile("test")
@ConditionalOnProperty(
    name = "spring.ai.openai.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class TestAIConfig {
  // AI сервисы будут отключены в тестах
}
