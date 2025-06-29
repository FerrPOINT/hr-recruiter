package azhukov.repository;

import azhukov.entity.Tariff;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Репозиторий для работы с тарифными планами */
@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

  /** Найти все активные тарифы */
  List<Tariff> findByIsActiveTrueOrderByPriceAsc();

  /** Найти тариф по названию */
  Optional<Tariff> findByNameIgnoreCase(String name);

  /** Проверить существование тарифа по названию */
  boolean existsByNameIgnoreCase(String name);

  /** Найти тарифы в диапазоне цен */
  @Query(
      "SELECT t FROM Tariff t WHERE t.isActive = true AND t.price BETWEEN ?1 AND ?2 ORDER BY t.price ASC")
  List<Tariff> findActiveTariffsByPriceRange(Double minPrice, Double maxPrice);

  /** Найти тарифы с AI функциями */
  List<Tariff> findByIsActiveTrueAndAiFeaturesEnabledTrueOrderByPriceAsc();

  /** Найти самый дешевый активный тариф */
  @Query("SELECT t FROM Tariff t WHERE t.isActive = true ORDER BY t.price ASC LIMIT 1")
  Optional<Tariff> findCheapestActiveTariff();
}
