package azhukov.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AiOpenApiMapper {
  AiOpenApiMapper INSTANCE = Mappers.getMapper(AiOpenApiMapper.class);

  @Mapping(target = "existingData", source = "existingData")
  azhukov.model.PositionDataGenerationRequest toModel(
      azhukov.model.PositionDataGenerationRequest request);

  azhukov.model.PositionDataGenerationResponse toModel(
      azhukov.model.PositionDataGenerationResponse dto);

  azhukov.model.PositionDataGenerationRequest toDto(
      azhukov.model.PositionDataGenerationRequest request);

  azhukov.model.PositionDataGenerationResponse toDto(
      azhukov.model.PositionDataGenerationResponse model);
}
