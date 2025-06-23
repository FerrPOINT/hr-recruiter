package azhukov.util;

import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Утилитный класс для работы с пагинацией и сортировкой. */
public class PageableUtils {

  /**
   * Создает Pageable из Optional параметров пагинации и сортировки.
   *
   * @param page номер страницы (по умолчанию 0)
   * @param size размер страницы (по умолчанию 20)
   * @param sort поле для сортировки (по умолчанию "createdAt")
   * @return Pageable объект
   */
  public static Pageable fromOptionals(
      Optional<Integer> page, Optional<Integer> size, Optional<String> sort) {

    int pageNumber = page.orElse(0);
    int pageSize = size.orElse(20);
    String sortField = sort.orElse("createdAt");

    return PageRequest.of(pageNumber, pageSize, Sort.by(sortField));
  }

  /**
   * Создает Pageable с сортировкой по убыванию.
   *
   * @param page номер страницы (по умолчанию 0)
   * @param size размер страницы (по умолчанию 20)
   * @param sort поле для сортировки (по умолчанию "createdAt")
   * @return Pageable объект с сортировкой по убыванию
   */
  public static Pageable fromOptionalsDesc(
      Optional<Integer> page, Optional<Integer> size, Optional<String> sort) {

    int pageNumber = page.orElse(0);
    int pageSize = size.orElse(20);
    String sortField = sort.orElse("createdAt");

    return PageRequest.of(pageNumber, pageSize, Sort.by(sortField).descending());
  }
}
