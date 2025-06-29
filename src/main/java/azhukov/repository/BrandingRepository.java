package azhukov.repository;

import azhukov.entity.Branding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Репозиторий для работы с настройками брендинга */
@Repository
public interface BrandingRepository extends JpaRepository<Branding, Long> {

  /**
   * Найти первую запись настроек брендинга
   *
   * @return опциональная запись настроек брендинга
   */
  Optional<Branding> findFirstByOrderByIdAsc();
}
