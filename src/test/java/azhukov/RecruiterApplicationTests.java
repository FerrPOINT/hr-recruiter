package azhukov;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Базовые тесты для проверки работоспособности приложения. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class RecruiterApplicationTests {

  @Test
  void contextLoads() {
    // Проверяем, что контекст Spring загружается успешно
  }
}
