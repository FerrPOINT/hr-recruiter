package azhukov.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import azhukov.entity.UserEntity;
import azhukov.model.BaseUserFields;
import azhukov.model.RoleEnum;
import azhukov.model.User;
import azhukov.model.UserCreateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private final UserMapperImpl mapper = new UserMapperImpl();

  @Test
  void toDto_and_toEntity_basicMapping() {
    UserEntity entity =
        UserEntity.builder()
            .id(123L)
            .firstName("Ivan")
            .lastName("Ivanov")
            .email("ivan@example.com")
            .role(UserEntity.Role.RECRUITER)
            .avatarUrl("http://avatar")
            .language("ru")
            .active(true)
            .build();

    User dto = mapper.toDto(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getFirstName()).isEqualTo("Ivan");
    assertThat(dto.getLastName()).isEqualTo("Ivanov");
    assertThat(dto.getEmail()).isEqualTo("ivan@example.com");
    assertThat(dto.getRole()).isEqualTo(RoleEnum.RECRUITER);
    assertThat(dto.getAvatarUrl()).isEqualTo("http://avatar");
    assertThat(dto.getLanguage()).isEqualTo("ru");

    // Обратный маппинг (UserCreateRequest -> UserEntity)
    UserCreateRequest req =
        new UserCreateRequest()
            .firstName("Petr")
            .lastName("Petrov")
            .email("petr@example.com")
            .role(RoleEnum.ADMIN)
            .password("secret");
    UserEntity entity2 = mapper.toEntity(req);
    assertThat(entity2).isNotNull();
    assertThat(entity2.getFirstName()).isEqualTo("Petr");
    assertThat(entity2.getLastName()).isEqualTo("Petrov");
    assertThat(entity2.getEmail()).isEqualTo("petr@example.com");
    assertThat(entity2.getRole()).isEqualTo(UserEntity.Role.ADMIN);
  }

  @Test
  void updateEntityFromRequest_shouldUpdateFields() {
    UserEntity entity =
        UserEntity.builder()
            .firstName("Old")
            .lastName("Name")
            .email("old@example.com")
            .role(UserEntity.Role.RECRUITER)
            .build();
    BaseUserFields req = new BaseUserFields().firstName("New").lastName("Surname");
    mapper.updateEntityFromRequest(req, entity);
    assertThat(entity.getFirstName()).isEqualTo("New");
    assertThat(entity.getLastName()).isEqualTo("Surname");
    assertThat(entity.getEmail()).isEqualTo("old@example.com"); // не должен меняться
  }

  @Test
  void toDto_shouldReturnNullOnNull() {
    assertThat(mapper.toDto(null)).isNull();
  }

  @Test
  void toDtoList_shouldMapList() {
    UserEntity e1 =
        UserEntity.builder()
            .firstName("A")
            .lastName("B")
            .email("a@b")
            .role(UserEntity.Role.ADMIN)
            .build();
    UserEntity e2 =
        UserEntity.builder()
            .firstName("C")
            .lastName("D")
            .email("c@d")
            .role(UserEntity.Role.RECRUITER)
            .build();
    List<User> dtos = mapper.toDtoList(List.of(e1, e2));
    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getFirstName()).isEqualTo("A");
    assertThat(dtos.get(1).getFirstName()).isEqualTo("C");
  }
}
