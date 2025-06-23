package azhukov.controller;

import azhukov.api.TeamUsersApi;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.UserMapper;
import azhukov.model.BaseUserFields;
import azhukov.model.User;
import azhukov.model.UserCreateRequest;
import azhukov.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления пользователями и командой Реализует интерфейс TeamUsersApi,
 * сгенерированный по OpenAPI спецификации
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Team & Users", description = "Управление командой и пользователями")
public class UsersApiController implements TeamUsersApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<List<User>> listUsers() {
    log.debug("Getting all users");

    // Получаем первую страницу со всеми пользователями
    Page<User> usersPage = userService.findAllUsers(PageRequest.of(0, Integer.MAX_VALUE));
    List<User> users = usersPage.getContent();

    return ResponseEntity.ok(users);
  }

  @Override
  public ResponseEntity<User> createUser(UserCreateRequest userCreateRequest) {
    log.debug("Creating new user: {}", userCreateRequest.getEmail());

    User createdUser = userService.createUser(userCreateRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @Override
  public ResponseEntity<User> getUser(Long id) {
    log.info("Getting user with id: {}", id);
    try {
      User user = userService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (ResourceNotFoundException e) {
      log.warn("User not found with id: {}", id);
      throw e;
    }
  }

  @Override
  public ResponseEntity<User> updateUser(Long id, BaseUserFields userUpdateRequest) {
    log.info("Updating user with id: {}", id);
    User updatedUser = userService.updateUser(id, userUpdateRequest);
    return ResponseEntity.ok(updatedUser);
  }

  @Override
  public ResponseEntity<Void> deleteUser(Long id) {
    log.info("Deleting user with id: {}", id);
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<User>> getTeam() {
    log.debug("Getting team information");

    // Получаем всех активных пользователей как команду
    List<User> team = userService.findUsersByRole(azhukov.entity.UserEntity.Role.RECRUITER);
    return ResponseEntity.ok(team);
  }

  /** Получает список всех пользователей с пагинацией */
  @GetMapping
  @Operation(
      summary = "Список пользователей",
      description = "Возвращает список всех пользователей в системе с поддержкой пагинации")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный ответ",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<User>> listUsers(
      @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Поле для сортировки") @RequestParam(defaultValue = "createdAt")
          String sort,
      @Parameter(description = "Направление сортировки") @RequestParam(defaultValue = "desc")
          String direction) {

    log.debug(
        "Getting users list with page={}, size={}, sort={}, direction={}",
        page,
        size,
        sort,
        direction);

    Sort.Direction sortDirection =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

    Page<User> users = userService.findAllUsers(pageable);
    return ResponseEntity.ok(users);
  }

  /** Поиск пользователей */
  @GetMapping("/search")
  @Operation(
      summary = "Поиск пользователей",
      description = "Поиск пользователей по имени или email")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный ответ",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<User>> searchUsers(
      @Parameter(description = "Поисковый запрос") @RequestParam String query,
      @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size) {

    log.debug("Searching users with query: {}", query);

    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userService.searchUsers(query, pageable);
    return ResponseEntity.ok(users);
  }

  /** Получает пользователей по роли */
  @GetMapping("/by-role/{role}")
  @Operation(
      summary = "Пользователи по роли",
      description = "Возвращает список пользователей с указанной ролью")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный ответ",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<User>> getUsersByRole(
      @Parameter(description = "Роль пользователей") @PathVariable String role) {

    log.debug("Getting users by role: {}", role);

    // Преобразуем строку в enum
    azhukov.entity.UserEntity.Role userRole =
        azhukov.entity.UserEntity.Role.valueOf(role.toUpperCase());
    List<User> users = userService.findUsersByRole(userRole);
    return ResponseEntity.ok(users);
  }

  /** Активирует/деактивирует пользователя */
  @PatchMapping("/{id}/toggle-status")
  @Operation(
      summary = "Переключить статус пользователя",
      description = "Активирует или деактивирует пользователя")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус изменен",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
      })
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<User> toggleUserStatus(
      @Parameter(description = "ID пользователя") @PathVariable Long id) {

    log.debug("Toggling status for user with ID: {}", id);

    User user = userService.toggleUserStatus(id);
    return ResponseEntity.ok(user);
  }
}
