package azhukov.service;

import static org.assertj.core.api.Assertions.assertThat;

import azhukov.entity.UserEntity;
import azhukov.model.RoleEnum;
import azhukov.model.UserCreateRequest;
import azhukov.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class UserServiceTest {

  @Autowired UserService userService;
  @Autowired UserRepository userRepository;

  @Test
  @DisplayName("Создаёт пользователя и находит по email")
  void createAndFindUser() {
    UserCreateRequest req =
        new UserCreateRequest()
            .firstName("Anna")
            .lastName("Ivanova")
            .email("anna@example.com")
            .role(RoleEnum.RECRUITER)
            .password("pass");
    userService.createUser(req);
    UserEntity found = userRepository.findByEmail("anna@example.com").orElse(null);
    assertThat(found).isNotNull();
    assertThat(found.getFirstName()).isEqualTo("Anna");
    assertThat(found.getRole()).isEqualTo(UserEntity.Role.RECRUITER);
  }
}
