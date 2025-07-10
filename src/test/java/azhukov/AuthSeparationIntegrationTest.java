package azhukov;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import azhukov.repository.PositionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestDataInitializer.class)
class AuthSeparationIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PositionRepository positionRepository;

  @Test
  void adminToken_canAccessAllEndpoints() throws Exception {
    String adminToken = obtainAdminToken();
    Long positionId = getTestPositionId();

    // Админ может получить список вакансий
    mockMvc
        .perform(get("/positions").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    // Админ может получить список кандидатов (админ имеет доступ ко всем эндпоинтам)
    mockMvc
        .perform(get("/candidates").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void candidateToken_hasLimitedAccess() throws Exception {
    String candidateToken = obtainCandidateToken();
    Long positionId = getTestPositionId();

    // Кандидат НЕ может получить список всех кандидатов (это админская функция)
    mockMvc
        .perform(get("/candidates").header("Authorization", "Bearer " + candidateToken))
        .andExpect(status().isForbidden());

    // Кандидат НЕ может получить список всех вакансий (это админская функция)
    mockMvc
        .perform(get("/positions").header("Authorization", "Bearer " + candidateToken))
        .andExpect(status().isForbidden());

    // Кандидат может получить данные по конкретной вакансии
    mockMvc
        .perform(
            get("/positions/" + positionId).header("Authorization", "Bearer " + candidateToken))
        .andExpect(status().isOk());

    // Кандидат НЕ может создавать вакансии
    mockMvc
        .perform(
            post("/positions")
                .header("Authorization", "Bearer " + candidateToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Test Position\",\"description\":\"Test\",\"status\":\"active\"}"))
        .andExpect(status().isForbidden());

    // Кандидат НЕ может изменять вакансии
    mockMvc
        .perform(
            put("/positions/" + positionId)
                .header("Authorization", "Bearer " + candidateToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Position\",\"description\":\"Updated\"}"))
        .andExpect(status().isForbidden());

    // Кандидат НЕ может удалять вакансии (метод DELETE не существует в API)
    // Убираем этот тест, так как метод DELETE не определен в OpenAPI спецификации

    // Кандидат не может получить доступ к админским эндпоинтам (например, управление
    // пользователями)
    mockMvc
        .perform(get("/users").header("Authorization", "Bearer " + candidateToken))
        .andExpect(status().isForbidden());
  }

  // Вспомогательные методы для получения токенов
  private String obtainAdminToken() throws Exception {
    var mvcResult =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@example.com\",\"password\":\"adminpass\"}"))
            .andReturn();
    int status = mvcResult.getResponse().getStatus();
    String response = mvcResult.getResponse().getContentAsString();
    System.out.println("ADMIN LOGIN RESPONSE: status=" + status + ", body=" + response);
    if (status != 200)
      throw new AssertionError("Admin login failed: status=" + status + ", body=" + response);
    return JsonPath.read(response, "$.token");
  }

  private String obtainCandidateToken() throws Exception {
    var mvcResult =
        mockMvc
            .perform(
                post("/candidates/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"test@user.com\",\"phone\":\"+79990000000\",\"positionId\":"
                            + getTestPositionId()
                            + "}"))
            .andReturn();
    int status = mvcResult.getResponse().getStatus();
    String response = mvcResult.getResponse().getContentAsString();
    System.out.println("CANDIDATE AUTH RESPONSE: status=" + status + ", body=" + response);
    if (status != 200)
      throw new AssertionError("Candidate auth failed: status=" + status + ", body=" + response);
    return JsonPath.read(response, "$.token");
  }

  private Long getTestPositionId() {
    return positionRepository.findAll().get(0).getId();
  }
}
