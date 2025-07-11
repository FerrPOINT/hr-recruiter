package azhukov.controller;

import azhukov.api.DefaultApi;
import azhukov.model.GetLearn200ResponseInner;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для дополнительных эндпоинтов (архив, обучающие материалы). Реализует интерфейс
 * DefaultApi, сгенерированный из OpenAPI спецификации.
 */
@RestController
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DefaultController extends BaseController implements DefaultApi {

  @Override
  public ResponseEntity<List<Object>> getArchive() {
    log.info("Getting archive data");

    // Возвращаем пустой список архивных данных
    // В будущем здесь будет логика получения архивных интервью и кандидатов
    return ResponseEntity.ok(List.of());
  }

  @Override
  public ResponseEntity<List<GetLearn200ResponseInner>> getLearn() {
    log.info("Getting learning materials");

    // Возвращаем базовые обучающие материалы
    // В будущем здесь будет логика получения материалов из базы данных
    GetLearn200ResponseInner material = new GetLearn200ResponseInner();
    material.setTitle("Добро пожаловать в HR Recruiter");
    material.setDescription("Руководство по использованию платформы");
    material.setUrl("https://docs.hr-recruiter.com/getting-started");

    return ResponseEntity.ok(List.of(material));
  }
}
