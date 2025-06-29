package azhukov.mapper;

import azhukov.entity.Branding;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/** Маппер для преобразования между сущностью Branding и openapi-моделью */
@Mapper(componentModel = "spring")
public interface BrandingMapper extends CommonMapper {

  /** Преобразовать сущность в openapi-модель */
  azhukov.model.Branding toDto(Branding branding);

  /** Создать сущность из openapi-модели */
  Branding toEntity(azhukov.model.BrandingUpdateRequest request);

  /** Обновить сущность из openapi-модели */
  void updateEntity(@MappingTarget Branding branding, azhukov.model.BrandingUpdateRequest request);
}
