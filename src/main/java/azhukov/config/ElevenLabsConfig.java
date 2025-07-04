package azhukov.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/** Конфигурация для ElevenLabs интеграции */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ElevenLabsConfig {

  private final ElevenLabsProperties properties;

  /** RestTemplate с настройками для ElevenLabs API и логированием ошибок */
  @Bean
  public RestTemplate elevenLabsRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) properties.getTimeout().toMillis());
    factory.setReadTimeout((int) properties.getTimeout().toMillis());

    RestTemplate restTemplate = new RestTemplate(factory);
    restTemplate.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().isError();
          }

          @Override
          public void handleError(ClientHttpResponse response) throws IOException {
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            log.error("ElevenLabs API error: status={}, body={}", response.getStatusCode(), body);
          }
        });
    return restTemplate;
  }

  // ObjectMapper теперь конфигурируется глобально в JacksonConfig
}
