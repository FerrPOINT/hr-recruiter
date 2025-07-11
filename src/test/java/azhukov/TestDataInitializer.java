package azhukov;

import azhukov.entity.Position;
import azhukov.entity.UserEntity;
import azhukov.repository.PositionRepository;
import azhukov.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer {

  private final UserRepository userRepository;
  private final PositionRepository positionRepository;
  private final PasswordEncoder passwordEncoder;

  public static Long TEST_POSITION_ID;

  @PostConstruct
  public void init() {
    // Тестовый админ
    UserEntity admin = userRepository.findByEmail("admin@example.com").orElse(null);
    if (admin == null) {
      admin = new UserEntity();
      admin.setEmail("admin@example.com");
      String rawPassword = "admin";
      String hash = passwordEncoder.encode(rawPassword);
      admin.setPassword(hash);
      admin.setRole(UserEntity.Role.ADMIN);
      admin.setFirstName("Admin");
      admin.setLastName("User");
      admin.setActive(true);
      admin = userRepository.save(admin);
      System.out.println(
          "[TestDataInitializer] Created test admin: email="
              + admin.getEmail()
              + ", rawPassword="
              + rawPassword
              + ", hash="
              + hash
              + ", active="
              + admin.isActive()
              + ", id="
              + admin.getId());
    } else {
      System.out.println(
          "[TestDataInitializer] Test admin already exists: email="
              + admin.getEmail()
              + ", hash="
              + admin.getPassword()
              + ", active="
              + admin.isActive()
              + ", id="
              + admin.getId());
    }
    // Тестовая вакансия
    if (positionRepository.count() == 0) {
      Position position = new Position();
      position.setTitle("Тестовая вакансия");
      position.setStatus(Position.Status.ACTIVE);
      position.setCreatedBy(admin);
      position.setDescription("Для интеграционных тестов");
      position.setCompany("TestCompany");
      position.setLanguage("ru");
      position.setAnswerTime(60);
      position.setLevel(Position.Level.JUNIOR);
      position.setSaveAudio(true);
      position.setSaveVideo(false);
      position.setRandomOrder(false);
      position.setQuestionType("BASIC");
      position.setQuestionsCount(3);
      position.setCheckType("AUTO");
      positionRepository.save(position);
    }
  }
}
