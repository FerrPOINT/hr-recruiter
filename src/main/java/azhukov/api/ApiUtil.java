package azhukov.api;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Утилитарный класс для API операций. Используется сгенерированными OpenAPI интерфейсами. */
public class ApiUtil {

  /**
   * Устанавливает пример ответа для запроса.
   *
   * @param request HTTP запрос
   * @param contentType тип контента
   * @param example пример ответа
   */
  public static void setExampleResponse(Object request, String contentType, String example) {
    // Метод для совместимости с сгенерированным кодом
    // В реальной реализации здесь может быть логика установки примера
  }

  /**
   * Создает URI для ресурса.
   *
   * @param path путь к ресурсу
   * @return URI
   */
  public static URI createUri(String path) {
    return ServletUriComponentsBuilder.fromCurrentRequest().path(path).build().toUri();
  }
}
