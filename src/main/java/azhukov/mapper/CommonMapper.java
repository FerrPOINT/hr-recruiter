package azhukov.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/**
 * Универсальный маппер с общими методами для всех маппингов. Содержит стандартные преобразования
 * типов данных.
 */
@Mapper(componentModel = "spring")
public interface CommonMapper {

  /** Преобразование LocalDateTime в OffsetDateTime */
  default OffsetDateTime map(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    return localDateTime.atOffset(ZoneOffset.UTC);
  }

  /** Преобразование OffsetDateTime в LocalDateTime */
  default LocalDateTime map(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return null;
    }
    return offsetDateTime.toLocalDateTime();
  }

  /** Преобразование UUID в String */
  default String map(UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return uuid.toString();
  }

  /** Преобразование String в UUID */
  default UUID map(String string) {
    if (string == null || string.trim().isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(string);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Преобразование List<UUID> в List<String> */
  default List<String> mapUuidList(List<UUID> uuidList) {
    if (uuidList == null) {
      return null;
    }
    return uuidList.stream().map(this::map).toList();
  }

  /** Преобразование List<String> в List<UUID> */
  default List<UUID> mapStringList(List<String> stringList) {
    if (stringList == null) {
      return null;
    }
    return stringList.stream().map(this::map).filter(uuid -> uuid != null).toList();
  }
}
