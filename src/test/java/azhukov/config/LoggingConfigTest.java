package azhukov.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"app.debug.enabled=true"})
@Disabled("Интеграционный тест - требует базу данных")
class LoggingConfigTest {

  @Test
  void contextLoads() {
    // Проверяем что контекст загружается с конфигурацией логирования
    assertTrue(true);
  }
}
