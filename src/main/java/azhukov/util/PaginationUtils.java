package azhukov.util;

import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Утилиты для работы с пагинацией. Предоставляет методы для создания Pageable объектов и заполнения
 * полей пагинации в ответах.
 */
@Slf4j
public class PaginationUtils {

  // ========== СОЗДАНИЕ PAGEABLE ==========

  /**
   * Создает Pageable с сортировкой по возрастанию.
   *
   * @param pageNumber Номер страницы
   * @param pageSize Размер страницы
   * @param sortField Поле для сортировки
   * @return Pageable объект
   */
  public static Pageable createPageable(int pageNumber, int pageSize, String sortField) {
    return PageRequest.of(pageNumber, pageSize, Sort.by(sortField));
  }

  /**
   * Создает Pageable с сортировкой по убыванию.
   *
   * @param pageNumber Номер страницы
   * @param pageSize Размер страницы
   * @param sortField Поле для сортировки
   * @return Pageable объект
   */
  public static Pageable createPageableDesc(int pageNumber, int pageSize, String sortField) {
    return PageRequest.of(pageNumber, pageSize, Sort.by(sortField).descending());
  }

  /**
   * Создает Pageable из Optional параметров контроллера с дефолтными значениями.
   *
   * @param page Optional номер страницы
   * @param size Optional размер страницы
   * @return Pageable объект
   */
  public static Pageable createPageableFromOptional(Optional<Long> page, Optional<Long> size) {
    int pageNum = page.orElse(0L).intValue();
    int pageSize = size.orElse(20L).intValue();
    return PageRequest.of(pageNum, pageSize);
  }

  /**
   * Создает Pageable из Optional параметров контроллера с сортировкой.
   *
   * @param page Optional номер страницы
   * @param size Optional размер страницы
   * @param sort Optional поле сортировки
   * @return Pageable объект
   */
  public static Pageable createPageableFromOptional(
      Optional<Long> page, Optional<Long> size, Optional<String> sort) {
    int pageNum = page.orElse(0L).intValue();
    int pageSize = size.orElse(20L).intValue();

    if (sort.isPresent() && !sort.get().trim().isEmpty()) {
      return PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, sort.get()));
    }

    return PageRequest.of(pageNum, pageSize);
  }

  /**
   * Создает Pageable из Optional параметров контроллера с кастомной сортировкой.
   *
   * @param page Optional номер страницы
   * @param size Optional размер страницы
   * @param sort Optional поле сортировки
   * @param defaultSortField Поле сортировки по умолчанию
   * @return Pageable объект
   */
  public static Pageable createPageableFromOptional(
      Optional<Long> page, Optional<Long> size, Optional<String> sort, String defaultSortField) {
    int pageNum = page.orElse(0L).intValue();
    int pageSize = size.orElse(20L).intValue();

    String sortField = sort.orElse(defaultSortField);
    return PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, sortField));
  }

  // ========== ЗАПОЛНЕНИЕ ПОЛЕЙ ПАГИНАЦИИ ==========

  /** Интерфейс для пагинированных ответов. */
  public interface PaginatedResponseInterface {
    void setContent(Object content);

    void setTotalElements(Long totalElements);

    void setTotalPages(Long totalPages);

    void setNumber(Long number);

    void setSize(Long size);
  }

  /** Базовый класс для пагинированных ответов. */
  @Data
  public static class PaginatedResponse implements PaginatedResponseInterface {
    private Object content;
    private Long totalElements;
    private Long totalPages;
    private Long number;
    private Long size;
  }

  /**
   * Заполняет поля пагинации в ответе, используя данные из Page объекта.
   *
   * @param page Page объект с данными
   * @param response Ответ для заполнения полей пагинации
   */
  public static void fillPaginationFields(Page<?> page, Object response) {
    try {
      // Используем рефлексию для установки полей
      var responseClass = response.getClass();

      var totalElementsField = responseClass.getDeclaredField("totalElements");
      totalElementsField.setAccessible(true);
      totalElementsField.set(response, page.getTotalElements());

      var totalPagesField = responseClass.getDeclaredField("totalPages");
      totalPagesField.setAccessible(true);
      totalPagesField.set(response, (long) page.getTotalPages());

      var numberField = responseClass.getDeclaredField("number");
      numberField.setAccessible(true);
      numberField.set(response, (long) page.getNumber());

      var sizeField = responseClass.getDeclaredField("size");
      sizeField.setAccessible(true);
      sizeField.set(response, (long) page.getSize());

    } catch (Exception e) {
      log.error("Failed to fill pagination fields", e);
    }
  }

  /**
   * Заполняет поля пагинации в объекте, реализующем интерфейс PaginatedResponseInterface.
   *
   * @param page Spring Page с данными
   * @param response Объект пагинации
   * @return Заполненный объект
   */
  public static <T extends PaginatedResponseInterface> T fillPaginationFields(
      Page<?> page, T response) {
    response.setContent(page.getContent());
    response.setTotalElements(page.getTotalElements());
    response.setTotalPages((long) page.getTotalPages());
    response.setNumber((long) page.getNumber());
    response.setSize((long) page.getSize());
    return response;
  }

  /**
   * Заполняет поля пагинации в базовом объекте.
   *
   * @param page Spring Page с данными
   * @param response Объект пагинации
   * @return Заполненный объект
   */
  public static PaginatedResponse fillPaginationFields(Page<?> page, PaginatedResponse response) {
    response.setContent(page.getContent());
    response.setTotalElements(page.getTotalElements());
    response.setTotalPages((long) page.getTotalPages());
    response.setNumber((long) page.getNumber());
    response.setSize((long) page.getSize());
    return response;
  }

  /**
   * Заполняет поля пагинации в сгенерированном PaginatedResponse.
   *
   * @param page Spring Page с данными
   * @param response Сгенерированный PaginatedResponse
   * @return Заполненный объект
   */
  public static azhukov.model.PaginatedResponse fillPaginationFields(
      Page<?> page, azhukov.model.PaginatedResponse response) {
    response.setContent((java.util.List<Object>) page.getContent());
    response.setTotalElements(page.getTotalElements());
    response.setTotalPages((long) page.getTotalPages());
    response.setNumber((long) page.getNumber());
    response.setSize((long) page.getSize());
    return response;
  }

  /** Адаптер для GetAllQuestions200Response. */
  public static class QuestionsPaginatedAdapter implements PaginatedResponseInterface {
    private final azhukov.model.GetAllQuestions200Response response;

    public QuestionsPaginatedAdapter(azhukov.model.GetAllQuestions200Response response) {
      this.response = response;
    }

    @Override
    public void setContent(Object content) {
      response.setContent((java.util.List<azhukov.model.Question>) content);
    }

    @Override
    public void setTotalElements(Long totalElements) {
      response.setTotalElements(totalElements);
    }

    @Override
    public void setTotalPages(Long totalPages) {
      response.setTotalPages(totalPages);
    }

    @Override
    public void setNumber(Long number) {
      response.setNumber(number);
    }

    @Override
    public void setSize(Long size) {
      response.setSize(size);
    }

    public azhukov.model.GetAllQuestions200Response getResponse() {
      return response;
    }
  }

  /**
   * Удобный метод для заполнения GetAllQuestions200Response.
   *
   * @param page Spring Page с данными
   * @param response GetAllQuestions200Response
   * @return Заполненный объект
   */
  public static azhukov.model.GetAllQuestions200Response fillQuestionsPagination(
      Page<?> page, azhukov.model.GetAllQuestions200Response response) {
    QuestionsPaginatedAdapter adapter = new QuestionsPaginatedAdapter(response);
    fillPaginationFields(page, adapter);
    return adapter.getResponse();
  }
}
