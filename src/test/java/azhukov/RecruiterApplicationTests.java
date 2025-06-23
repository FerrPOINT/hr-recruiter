package azhukov;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Базовые тесты для проверки работоспособности приложения. */
@SpringBootTest
@ActiveProfiles("test")
class RecruiterApplicationTests {

  @Test
  void contextLoads() {
    // Проверяем, что контекст Spring загружается успешно
  }
}
