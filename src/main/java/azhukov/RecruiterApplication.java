package azhukov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Главный класс Spring Boot приложения HR Recruiter.
 *
 * <p>Основные возможности: - Управление вакансиями и кандидатами - Проведение AI-собеседований -
 * Аналитика и отчеты - Управление командой
 *
 * <p>Технологический стек: - Spring Boot 3.4.0 - Java 22 - PostgreSQL - Spring Security - Spring
 * Data JPA - MapStruct - Lombok - OpenAPI 3.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class RecruiterApplication {

  public static void main(String[] args) {
    SpringApplication.run(RecruiterApplication.class, args);
  }
}
