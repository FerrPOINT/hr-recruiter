package azhukov;

import static org.junit.jupiter.api.Assertions.*;

import azhukov.entity.UserEntity;
import azhukov.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Disabled("Интеграционный тест - требует базу данных")
class DefaultAdminUserTest {

  @Autowired private UserRepository userRepository;

  @Autowired private BCryptPasswordEncoder passwordEncoder;

  @Test
  void shouldCreateDefaultAdminUser() {
    // Проверяем что дефолтный админ создается
    Optional<UserEntity> adminUser = userRepository.findByEmail("admin@example.com");

    assertTrue(adminUser.isPresent(), "Default admin user should exist");

    UserEntity admin = adminUser.get();
    assertEquals("Admin", admin.getFirstName());
    assertEquals("User", admin.getLastName());
    assertEquals("admin@example.com", admin.getEmail());
    assertEquals(UserEntity.Role.ADMIN, admin.getRole());
    assertTrue(admin.isActive());
    assertEquals("ru", admin.getLanguage());

    // Проверяем что пароль работает
    assertTrue(
        passwordEncoder.matches("admin", admin.getPassword()), "Password should match 'admin'");
  }
}
