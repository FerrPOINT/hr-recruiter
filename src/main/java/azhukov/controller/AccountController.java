package azhukov.controller;

import azhukov.api.AccountApi;
import azhukov.model.BaseUserFields;
import azhukov.model.GetUserInfo200Response;
import azhukov.model.User;
import azhukov.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления аккаунтом текущего пользователя Реализует интерфейс AccountApi,
 * сгенерированный по OpenAPI спецификации
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

  private final UserService userService;

  @Override
  public ResponseEntity<User> getAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    log.debug("Getting account for user: {}", email);

    User user = userService.getCurrentUser(email);
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<User> updateAccount(BaseUserFields userUpdateRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    log.debug("Updating account for user: {}", email);

    User updatedUser = userService.updateCurrentUser(email, userUpdateRequest);
    return ResponseEntity.ok(updatedUser);
  }

  @Override
  public ResponseEntity<GetUserInfo200Response> getUserInfo() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    log.debug("Getting user info for user: {}", email);

    // Получаем основную информацию о пользователе
    User user = userService.getCurrentUser(email);

    // Создаем ответ с дополнительной информацией
    GetUserInfo200Response response = new GetUserInfo200Response();
    response.setPhone(""); // TODO: Добавить поле phone в UserEntity
    response.setPreferences("{}"); // TODO: Добавить поле preferences в UserEntity

    return ResponseEntity.ok(response);
  }
}
