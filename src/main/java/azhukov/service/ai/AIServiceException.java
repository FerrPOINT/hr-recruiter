package azhukov.service.ai;

/**
 * Исключение, возникающее при ошибках в AI сервисах. Следует принципу Single Responsibility (SOLID)
 * - отвечает только за обработку ошибок AI.
 *
 * @author AI Team
 * @version 1.0
 */
public class AIServiceException extends RuntimeException {

  private final ErrorType errorType;
  private final String serviceName;

  /** Типы ошибок AI сервиса. */
  public enum ErrorType {
    API_KEY_MISSING("Отсутствует API ключ"),
    API_UNAVAILABLE("AI сервис недоступен"),
    RATE_LIMIT_EXCEEDED("Превышен лимит запросов"),
    INVALID_REQUEST("Некорректный запрос"),
    RESPONSE_PARSING_ERROR("Ошибка парсинга ответа"),
    NETWORK_ERROR("Сетевая ошибка"),
    UNKNOWN_ERROR("Неизвестная ошибка");

    private final String description;

    ErrorType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Конструктор с сообщением об ошибке.
   *
   * @param message Сообщение об ошибке
   * @param errorType Тип ошибки
   * @param serviceName Название AI сервиса
   */
  public AIServiceException(String message, ErrorType errorType, String serviceName) {
    super(message);
    this.errorType = errorType;
    this.serviceName = serviceName;
  }

  /**
   * Конструктор с сообщением об ошибке и причиной.
   *
   * @param message Сообщение об ошибке
   * @param cause Причина ошибки
   * @param errorType Тип ошибки
   * @param serviceName Название AI сервиса
   */
  public AIServiceException(
      String message, Throwable cause, ErrorType errorType, String serviceName) {
    super(message, cause);
    this.errorType = errorType;
    this.serviceName = serviceName;
  }

  /**
   * Получает тип ошибки.
   *
   * @return Тип ошибки
   */
  public ErrorType getErrorType() {
    return errorType;
  }

  /**
   * Получает название сервиса.
   *
   * @return Название AI сервиса
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Получает описание ошибки.
   *
   * @return Описание ошибки
   */
  public String getErrorDescription() {
    return errorType.getDescription();
  }

  @Override
  public String toString() {
    return String.format(
        "AIServiceException{service='%s', errorType=%s, message='%s'}",
        serviceName, errorType, getMessage());
  }
}
