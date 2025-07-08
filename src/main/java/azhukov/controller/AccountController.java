package azhukov.controller;

import azhukov.api.AccountApi;
import azhukov.model.BaseUserFields;
import azhukov.model.GetUserInfo200Response;
import azhukov.model.User;
import azhukov.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления аккаунтом текущего пользователя. Реализует интерфейс AccountApi,
 * сгенерированный по OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class AccountController extends BaseController implements AccountApi {

  private final UserService userService;

  @Override
  public ResponseEntity<User> getAccount() {
    String email = getCurrentUserEmail();
    log.debug("Getting account for user: {}", email);

    User user = userService.getCurrentUser(email);
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<User> updateAccount(BaseUserFields userUpdateRequest) {
    String email = getCurrentUserEmail();
    log.debug("Updating account for user: {}", email);

    User updatedUser = userService.updateCurrentUser(email, userUpdateRequest);
    return ResponseEntity.ok(updatedUser);
  }

  @Override
  public ResponseEntity<GetUserInfo200Response> getUserInfo() {
    String email = getCurrentUserEmail();
    log.debug("Getting user info for user: {}", email);

    // Получаем основную информацию о пользователе
    User user = userService.getCurrentUser(email);

    // Создаем ответ с дополнительной информацией
    GetUserInfo200Response response = new GetUserInfo200Response();
    response.setPhone(user.getPhone() != null ? user.getPhone() : "");

    return ResponseEntity.ok(response);
  }
}
