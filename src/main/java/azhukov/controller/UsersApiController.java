package azhukov.controller;

import azhukov.api.TeamUsersApi;
import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.model.*;
import azhukov.service.UserService;
import azhukov.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
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
 * Контроллер для управления пользователями и командой. Реализует интерфейс TeamUsersApi,
 * сгенерированный по OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UsersApiController extends BaseController implements TeamUsersApi {

  private final UserService userService;

  @Override
  public ResponseEntity<PaginatedResponse> listUsers(
      Optional<Long> page, Optional<Long> size, Optional<String> sort) {
    log.debug("Getting users with page={}, size={}, sort={}", page, size, sort);

    long pageNum = page.orElse(0L);
    long pageSize = size.orElse(20L);
    String sortField = sort.orElse("createdAt");

    Pageable pageable =
        PageRequest.of((int) pageNum, (int) pageSize, Sort.by(Sort.Direction.DESC, sortField));

    Page<User> usersPage = userService.findAllUsers(pageable);

    PaginatedResponse response = new PaginatedResponse();
    PaginationUtils.fillPaginationFields(usersPage, response);

    return ResponseEntity.ok(response);
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
    List<User> team = userService.findUsersByRole(UserEntity.Role.RECRUITER);
    return ResponseEntity.ok(team);
  }
}
