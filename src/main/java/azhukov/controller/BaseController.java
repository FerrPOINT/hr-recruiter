package azhukov.controller;

import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Базовый контроллер с общими утилитами для всех контроллеров. Предоставляет стандартные методы для
 * получения текущего пользователя и обработки общих ошибок.
 */
@Slf4j
public abstract class BaseController {

  /** Получает email текущего аутентифицированного пользователя */
  protected String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  /** Обрабатывает общие исключения и возвращает соответствующий HTTP статус */
  protected ResponseEntity<Void> handleException(Exception e, String operation) {
    if (e instanceof ResourceNotFoundException) {
      log.warn("Resource not found during {}: {}", operation, e.getMessage());
      return ResponseEntity.notFound().build();
    } else if (e instanceof ValidationException) {
      log.warn("Validation error during {}: {}", operation, e.getMessage());
      return ResponseEntity.badRequest().build();
    } else {
      log.error("Error during {}: {}", operation, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Обрабатывает общие исключения и возвращает соответствующий HTTP статус с телом ответа */
  protected <T> ResponseEntity<T> handleExceptionWithBody(
      Exception e, String operation, T defaultBody) {
    if (e instanceof ResourceNotFoundException) {
      log.warn("Resource not found during {}: {}", operation, e.getMessage());
      return ResponseEntity.notFound().build();
    } else if (e instanceof ValidationException) {
      log.warn("Validation error during {}: {}", operation, e.getMessage());
      return ResponseEntity.badRequest().build();
    } else {
      log.error("Error during {}: {}", operation, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(defaultBody);
    }
  }

  /** Проверяет, что строка не пустая и не равна "all" */
  protected String normalizeFilter(String filter) {
    if (filter == null || "all".equals(filter) || filter.trim().isEmpty()) {
      return null;
    }
    return filter.trim();
  }

  /** Создает Pageable объект с сортировкой */
  protected org.springframework.data.domain.Pageable createPageable(
      int page, int size, String sort) {
    org.springframework.data.domain.Sort.Direction direction =
        "asc".equalsIgnoreCase(sort)
            ? org.springframework.data.domain.Sort.Direction.ASC
            : org.springframework.data.domain.Sort.Direction.DESC;
    return org.springframework.data.domain.PageRequest.of(
        page, size, org.springframework.data.domain.Sort.by(direction, sort));
  }
}
