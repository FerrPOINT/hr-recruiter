package azhukov.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Дополнительная конфигурация для логирования тела запросов. Включается только в режиме отладки
 * (app.debug.enabled=true).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.debug.enabled", havingValue = "true")
public class RequestLoggingConfig {

  @Bean
  public FilterRegistrationBean<CommonsRequestLoggingFilter> requestLoggingFilter() {
    FilterRegistrationBean<CommonsRequestLoggingFilter> registrationBean =
        new FilterRegistrationBean<>();
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

    // Настройки логирования
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(10000); // Максимальная длина тела запроса для логирования
    filter.setIncludeHeaders(true);
    filter.setAfterMessagePrefix("REQUEST DATA: ");
    filter.setAfterMessageSuffix("");

    registrationBean.setFilter(filter);
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(1); // После основного фильтра логирования

    log.info("Request body logging enabled for debugging");
    return registrationBean;
  }
}
