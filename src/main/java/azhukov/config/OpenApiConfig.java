package azhukov.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Конфигурация OpenAPI для документации API. Настраивает Swagger UI и генерацию документации. */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

  private final ApplicationProperties applicationProperties;

  /** Конфигурация OpenAPI */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .servers(servers())
        .components(components())
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
  }

  /** Информация об API */
  private Info apiInfo() {
    return new Info()
        .title("HR Recruiter API")
        .description(
            """
                Полная OpenAPI спецификация для платформы автоматизации HR-собеседований.

                ## Основные возможности:
                - Управление вакансиями и кандидатами
                - Проведение AI-собеседований
                - Аналитика и отчеты
                - Управление командой

                ## Аутентификация:
                Используется HTTP Basic Authentication с email и паролем.

                ## Роли пользователей:
                - **ADMIN**: Полный доступ ко всем функциям
                - **RECRUITER**: Создание вакансий, управление кандидатами
                - **VIEWER**: Только просмотр данных
                """)
        .version("1.0.0")
        .contact(
            new Contact()
                .name("HR Recruiter Team")
                .email("support@hr-recruiter.com")
                .url("https://hr-recruiter.com"))
        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));
  }

  /** Серверы API */
  private List<Server> servers() {
    return List.of(
        new Server().url("http://localhost:8080/api").description("Локальный сервер разработки"),
        new Server().url("https://api.hr-recruiter.com/api").description("Продакшн сервер"));
  }

  /** Компоненты API */
  private Components components() {
    return new Components()
        .addSecuritySchemes(
            "basicAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("HTTP Basic Authentication с email и паролем"));
  }
}
