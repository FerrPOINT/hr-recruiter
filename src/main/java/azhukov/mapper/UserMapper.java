package azhukov.mapper;

import azhukov.entity.UserEntity;
import azhukov.model.BaseUserFields;
import azhukov.model.User;
import azhukov.model.UserCreateRequest;
import java.util.List;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Маппер для преобразования между сущностями и DTO пользователей. Использует MapStruct для
 * автоматической генерации кода маппинга.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends CommonMapper {

  /** Преобразует UserEntity в User DTO */
  @Mapping(target = "id", source = "id")
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "avatarUrl", source = "avatarUrl")
  @Mapping(target = "language", source = "language")
  User toDto(UserEntity entity);

  /** Преобразует UserCreateRequest в UserEntity */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "password", source = "password")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "avatarUrl", ignore = true)
  @Mapping(target = "language", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  UserEntity toEntity(UserCreateRequest request);

  /** Обновляет UserEntity из BaseUserFields */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(BaseUserFields request, @MappingTarget UserEntity entity);

  /** Преобразует список UserEntity в список User DTO */
  List<User> toDtoList(List<UserEntity> entities);

  /** Преобразует Page<UserEntity> в Page<User> */
  default org.springframework.data.domain.Page<User> toDtoPage(
      org.springframework.data.domain.Page<UserEntity> page) {
    return page.map(this::toDto);
  }

  /** Маппинг ролей пользователей */
  @ValueMapping(source = "ADMIN", target = "ADMIN")
  @ValueMapping(source = "RECRUITER", target = "RECRUITER")
  @ValueMapping(source = "VIEWER", target = "VIEWER")
  azhukov.model.RoleEnum mapRole(UserEntity.Role role);

  /** Обратный маппинг ролей пользователей */
  @ValueMapping(source = "ADMIN", target = "ADMIN")
  @ValueMapping(source = "RECRUITER", target = "RECRUITER")
  @ValueMapping(source = "VIEWER", target = "VIEWER")
  UserEntity.Role mapRole(azhukov.model.RoleEnum role);

  /** Преобразование List<UUID> в List<UserEntity> (заглушка, будет заполнено в сервисе) */
  default List<UserEntity> mapTeam(List<UUID> teamIds) {
    return null; // Будет заполнено в сервисе через UserRepository
  }
}
