package azhukov.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Конфигурация Jackson с автоматической регистрацией всех доступных модулей */
@Configuration
public class JacksonConfig {

  /** Основной ObjectMapper с автоматической регистрацией всех модулей */
  @Bean
  @Primary
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper mapper = builder.build();
    // Автоматически регистрируем все доступные модули
    mapper.findAndRegisterModules();
    // Явно отключаем сериализацию дат как timestamps
    mapper.configure(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return mapper;
  }
}
