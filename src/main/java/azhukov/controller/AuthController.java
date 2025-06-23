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
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация и управление сессиями")
public class AuthController implements AuthApi {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;

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

      // Создаем ответ
      AuthResponse response = new AuthResponse();
      response.setUser(user);
      response.setToken("jwt-token-placeholder"); // TODO: Реализовать JWT токены

      log.info("User {} successfully logged in", loginRequest.getEmail());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Login failed for user: {}", loginRequest.getEmail(), e);
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
