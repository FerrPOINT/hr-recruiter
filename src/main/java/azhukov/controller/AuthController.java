package azhukov.controller;

import azhukov.api.AuthApi;
import azhukov.model.AuthResponse;
import azhukov.model.LoginRequest;
import azhukov.model.User;
import azhukov.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для аутентификации Реализует интерфейс AuthApi, сгенерированный по OpenAPI
 * спецификации
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация и управление сессиями")
@Slf4j
public class AuthController implements AuthApi {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;

  @Override
  public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
    log.info("=== AUTH CONTROLLER: Login attempt for user: {} ===", loginRequest.getEmail());

    try {
      // Аутентификация пользователя
      log.info("=== AUTH CONTROLLER: Starting authentication ===");
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getEmail(), loginRequest.getPassword()));
      log.info("=== AUTH CONTROLLER: Authentication successful ===");

      // Устанавливаем аутентификацию в контекст
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Получаем данные пользователя
      log.info("=== AUTH CONTROLLER: Getting user data ===");
      User user = userService.getCurrentUser(loginRequest.getEmail());

      // Создаем ответ
      AuthResponse response = new AuthResponse();
      response.setUser(user);
      response.setToken("jwt-token-placeholder"); // TODO: Реализовать JWT токены

      log.info("=== AUTH CONTROLLER: User {} successfully logged in ===", loginRequest.getEmail());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("=== AUTH CONTROLLER: Login failed for user: {} ===", loginRequest.getEmail(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
