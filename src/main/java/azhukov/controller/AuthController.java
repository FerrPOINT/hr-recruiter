package azhukov.controller;

import azhukov.api.AuthApi;
import azhukov.entity.UserEntity;
import azhukov.model.AuthResponse;
import azhukov.model.LoginRequest;
import azhukov.model.User;
import azhukov.service.JwtService;
import azhukov.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для аутентификации. Реализует интерфейс AuthApi, сгенерированный по OpenAPI
 * спецификации.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация и управление сессиями")
@Slf4j
public class AuthController extends BaseController implements AuthApi {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @Override
  public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
    log.info("Login attempt for user: {}", loginRequest.getEmail());

    try {
      // Аутентификация пользователя
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getEmail(), loginRequest.getPassword()));

      // Устанавливаем аутентификацию в контекст
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Получаем данные пользователя
      User user = userService.getCurrentUser(loginRequest.getEmail());
      UserEntity userEntity = userService.getUserEntityByEmail(loginRequest.getEmail());

      // Создаем ответ
      AuthResponse response = new AuthResponse();
      response.setUser(user);
      response.setToken(jwtService.generateAdminToken(userEntity));

      log.info("User {} successfully logged in", loginRequest.getEmail());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Login failed for user: {}", loginRequest.getEmail(), e);
      // Возвращаем JSON-ошибку в том же формате, что и RestAuthenticationEntryPoint
      var body = new java.util.HashMap<String, Object>();
      body.put("timestamp", java.time.LocalDateTime.now().toString());
      body.put("status", 401);
      body.put("error", "Unauthorized");
      body.put("message", "Неверные учетные данные");
      //noinspection unchecked
      return (ResponseEntity)
          ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body);
    }
  }

  @Override
  public ResponseEntity<Void> logout() {
    log.info("Logout request");

    // Очищаем контекст безопасности
    SecurityContextHolder.clearContext();

    log.info("User successfully logged out");
    return ResponseEntity.noContent().build();
  }
}
