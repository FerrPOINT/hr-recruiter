package azhukov.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Конфигурация для логирования HTTP запросов и ответов. Логирует метод, URL, статус код, время
 * выполнения и размер ответа.
 */
@Slf4j
@Configuration
public class LoggingConfig {

  @Bean
  public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
    FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RequestLoggingFilter());
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
  }

  /** Фильтр для логирования HTTP запросов и ответов */
  @Slf4j
  public static class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        chain.doFilter(request, response);
        return;
      }

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      // Пропускаем статические ресурсы и health checks
      String requestURI = httpRequest.getRequestURI();
      if (shouldSkipLogging(requestURI)) {
        chain.doFilter(request, response);
        return;
      }

      String requestId = UUID.randomUUID().toString().substring(0, 8);
      long startTime = System.currentTimeMillis();

      // Логируем входящий запрос
      logIncomingRequest(httpRequest, requestId);

      // Оборачиваем request и response для возможности чтения тела
      ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
      ContentCachingResponseWrapper wrappedResponse =
          new ContentCachingResponseWrapper(httpResponse);

      try {
        chain.doFilter(wrappedRequest, wrappedResponse);
      } finally {
        // Логируем исходящий ответ
        long duration = System.currentTimeMillis() - startTime;
        logOutgoingResponse(wrappedResponse, requestId, duration);

        // Копируем тело ответа обратно в оригинальный response
        wrappedResponse.copyBodyToResponse();
      }
    }

    private boolean shouldSkipLogging(String requestURI) {
      return requestURI.startsWith("/actuator/health")
          || requestURI.startsWith("/actuator/info")
          || requestURI.startsWith("/actuator/metrics")
          || requestURI.startsWith("/static/")
          || requestURI.startsWith("/favicon.ico")
          || requestURI.startsWith("/swagger-ui/")
          || requestURI.startsWith("/v3/api-docs");
    }

    private void logIncomingRequest(HttpServletRequest request, String requestId) {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String queryString = request.getQueryString();
      String userAgent = request.getHeader("User-Agent");
      String remoteAddr = getClientIpAddress(request);

      StringBuilder logMessage = new StringBuilder();
      logMessage.append("INCOMING REQUEST [").append(requestId).append("] ");
      logMessage.append(method).append(" ").append(uri);

      if (queryString != null && !queryString.isEmpty()) {
        logMessage.append("?").append(queryString);
      }

      logMessage.append(" | Client: ").append(remoteAddr);
      logMessage.append(" | User-Agent: ").append(userAgent != null ? userAgent : "Unknown");

      log.info(logMessage.toString());
    }

    private void logOutgoingResponse(
        ContentCachingResponseWrapper response, String requestId, long duration) {
      int status = response.getStatus();
      String contentType = response.getContentType();
      int contentLength = response.getContentSize();

      StringBuilder logMessage = new StringBuilder();
      logMessage.append("OUTGOING RESPONSE [").append(requestId).append("] ");
      logMessage.append("Status: ").append(status);
      logMessage.append(" | Duration: ").append(duration).append("ms");
      logMessage.append(" | Size: ").append(contentLength).append(" bytes");

      if (contentType != null) {
        logMessage.append(" | Content-Type: ").append(contentType);
      }

      // Логируем уровень в зависимости от статуса
      if (status >= 400) {
        log.warn(logMessage.toString());
      } else {
        log.info(logMessage.toString());
      }
    }

    private String getClientIpAddress(HttpServletRequest request) {
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null
          && !xForwardedFor.isEmpty()
          && !"unknown".equalsIgnoreCase(xForwardedFor)) {
        return xForwardedFor.split(",")[0].trim();
      }

      String xRealIp = request.getHeader("X-Real-IP");
      if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
        return xRealIp;
      }

      return request.getRemoteAddr();
    }
  }
}
