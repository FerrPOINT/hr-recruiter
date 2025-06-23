package azhukov.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import azhukov.entity.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class UserRepositoryTest {

  @Autowired UserRepository userRepository;

  @Test
  @DisplayName("Сохраняет и находит пользователя по email")
  void saveAndFindByEmail() {
    UserEntity user =
        UserEntity.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .email("ivan@example.com")
            .password("pass")
            .role(UserEntity.Role.RECRUITER)
            .build();
    userRepository.save(user);
    Optional<UserEntity> found = userRepository.findByEmail("ivan@example.com");
    assertThat(found).isPresent();
    assertThat(found.get().getFirstName()).isEqualTo("Ivan");
  }

  @Test
  @DisplayName("Не позволяет сохранить двух пользователей с одинаковым email")
  void uniqueEmailConstraint() {
    UserEntity user1 =
        UserEntity.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .email("dup@example.com")
            .password("pass")
            .role(UserEntity.Role.RECRUITER)
            .build();
    UserEntity user2 =
        UserEntity.builder()
            .firstName("Petr")
            .lastName("Petrov")
            .email("dup@example.com")
            .password("pass")
            .role(UserEntity.Role.ADMIN)
            .build();
    userRepository.save(user1);
    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          userRepository.saveAndFlush(user2);
        });
  }
}
