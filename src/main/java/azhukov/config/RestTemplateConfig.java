package azhukov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/** Конфигурация RestTemplate для HTTP-запросов к внешним API. */
@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    // Добавляем поддержку multipart для загрузки файлов
    restTemplate.getMessageConverters().add(new AllEncompassingFormHttpMessageConverter());

    return restTemplate;
  }
}
