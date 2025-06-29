package azhukov.mapper;

import azhukov.entity.Tariff;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/** Маппер для преобразования между сущностью Tariff и openapi-моделью */
@Mapper(componentModel = "spring")
public interface TariffMapper extends CommonMapper {

  /** Преобразовать сущность в openapi-модель */
  azhukov.model.Tariff toDto(Tariff tariff);

  /** Создать сущность из openapi-модели */
  Tariff toEntity(azhukov.model.TariffCreateRequest request);

  /** Обновить сущность из openapi-модели */
  void updateEntity(@MappingTarget Tariff tariff, azhukov.model.TariffUpdateRequest request);
}
