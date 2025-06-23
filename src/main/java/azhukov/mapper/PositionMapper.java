package azhukov.mapper;

import azhukov.entity.Position;
import azhukov.entity.UserEntity;
import azhukov.model.PositionCreateRequest;
import azhukov.model.PositionUpdateRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Маппер для преобразования между сущностями и DTO вакансий. Использует MapStruct для
 * автоматической генерации кода маппинга.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PositionMapper extends CommonMapper {

  /** Преобразует Position entity в Position DTO */
  @Mapping(target = "id", source = "id")
  @Mapping(target = "title", source = "title")
  @Mapping(target = "company", source = "company")
  @Mapping(target = "description", source = "description")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  @Mapping(target = "publicLink", source = "publicLink")
  @Mapping(target = "topics", source = "topics")
  @Mapping(target = "minScore", source = "minScore")
  @Mapping(target = "avgScore", source = "avgScore")
  @Mapping(target = "team", source = "team")
  @Mapping(target = "candidates", ignore = true) // Игнорируем для избежания циклических ссылок
  azhukov.model.Position toDto(Position entity);

  /** Преобразует PositionCreateRequest в Position entity */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "publicLink", ignore = true)
  @Mapping(target = "avgScore", ignore = true)
  @Mapping(target = "questions", ignore = true)
  @Mapping(target = "candidates", ignore = true)
  @Mapping(target = "interviews", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "team", ignore = true) // Будет заполнено отдельно
  Position toEntity(PositionCreateRequest request);

  /** Обновляет Position entity из PositionUpdateRequest */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "publicLink", ignore = true)
  @Mapping(target = "avgScore", ignore = true)
  @Mapping(target = "questions", ignore = true)
  @Mapping(target = "candidates", ignore = true)
  @Mapping(target = "interviews", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "team", ignore = true) // Будет заполнено отдельно
  void updateEntityFromRequest(PositionUpdateRequest request, @MappingTarget Position entity);

  /** Преобразует список Position entity в список Position DTO */
  List<azhukov.model.Position> toDtoList(List<Position> entities);

  /** Преобразует Page<Position> в Page<PositionDto> */
  default org.springframework.data.domain.Page<azhukov.model.Position> toDtoPage(
      org.springframework.data.domain.Page<Position> page) {
    return page.map(this::toDto);
  }

  /** Маппинг статусов вакансий */
  @ValueMapping(source = "ACTIVE", target = "ACTIVE")
  @ValueMapping(source = "PAUSED", target = "PAUSED")
  @ValueMapping(source = "ARCHIVED", target = "ARCHIVED")
  azhukov.model.PositionStatusEnum mapStatus(Position.Status status);

  /** Обратный маппинг статусов вакансий */
  @ValueMapping(source = "ACTIVE", target = "ACTIVE")
  @ValueMapping(source = "PAUSED", target = "PAUSED")
  @ValueMapping(source = "ARCHIVED", target = "ARCHIVED")
  Position.Status mapStatus(azhukov.model.PositionStatusEnum status);

  /** Преобразование List<UUID> в List<UserEntity> (заглушка, будет заполнено в сервисе) */
  default List<UserEntity> mapTeam(List<UUID> teamIds) {
    return null; // Будет заполнено в сервисе через UserRepository
  }

  // Методы для работы с Map (для API совместимости)
  /** Преобразует Position entity в Map */
  default Map<String, Object> toMap(Position entity) {
    if (entity == null) return null;

    Map<String, Object> map = new HashMap<>();
    map.put("id", entity.getId());
    map.put("title", entity.getTitle());
    map.put("company", entity.getCompany());
    map.put("description", entity.getDescription());
    map.put("status", entity.getStatus().name().toLowerCase());
    map.put("createdAt", entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
    map.put("updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
    map.put("publicLink", entity.getPublicLink());
    map.put("topics", entity.getTopics());
    map.put("minScore", entity.getMinScore());
    map.put("avgScore", entity.getAvgScore());
    map.put(
        "team",
        entity.getTeam() != null
            ? entity.getTeam().stream()
                .map(
                    user ->
                        Map.of(
                            "id", user.getId(),
                            "name", user.getFirstName() + " " + user.getLastName(),
                            "email", user.getEmail()))
                .collect(Collectors.toList())
            : null);

    return map;
  }

  /** Преобразует Map в Position entity */
  default Position toEntityFromMap(Map<String, Object> map) {
    if (map == null) return null;

    Position entity = new Position();
    if (map.containsKey("title")) {
      entity.setTitle((String) map.get("title"));
    }
    if (map.containsKey("company")) {
      entity.setCompany((String) map.get("company"));
    }
    if (map.containsKey("description")) {
      entity.setDescription((String) map.get("description"));
    }
    if (map.containsKey("status")) {
      String statusStr = (String) map.get("status");
      entity.setStatus(Position.Status.valueOf(statusStr.toUpperCase()));
    }
    if (map.containsKey("topics")) {
      @SuppressWarnings("unchecked")
      List<String> topics = (List<String>) map.get("topics");
      entity.setTopics(topics);
    }
    if (map.containsKey("minScore")) {
      Number minScore = (Number) map.get("minScore");
      entity.setMinScore(minScore != null ? minScore.doubleValue() : null);
    }

    return entity;
  }

  /** Обновляет Position entity из Map */
  default void updateEntityFromMap(Map<String, Object> map, Position entity) {
    if (map == null || entity == null) return;

    if (map.containsKey("title")) {
      entity.setTitle((String) map.get("title"));
    }
    if (map.containsKey("company")) {
      entity.setCompany((String) map.get("company"));
    }
    if (map.containsKey("description")) {
      entity.setDescription((String) map.get("description"));
    }
    if (map.containsKey("status")) {
      String statusStr = (String) map.get("status");
      entity.setStatus(Position.Status.valueOf(statusStr.toUpperCase()));
    }
    if (map.containsKey("topics")) {
      @SuppressWarnings("unchecked")
      List<String> topics = (List<String>) map.get("topics");
      entity.setTopics(topics);
    }
    if (map.containsKey("minScore")) {
      Number minScore = (Number) map.get("minScore");
      entity.setMinScore(minScore != null ? minScore.doubleValue() : null);
    }
  }
}
