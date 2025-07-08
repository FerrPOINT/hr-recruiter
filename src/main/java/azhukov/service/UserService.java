package azhukov.service;

import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.mapper.UserMapper;
import azhukov.model.BaseUserFields;
import azhukov.repository.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для управления пользователями. Расширяет BaseService для автоматизации общих операций. */
@Service
@Slf4j
public class UserService extends BaseService<UserEntity, Long, UserRepository> {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
    super(userRepository);
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  /** Создает нового пользователя */
  @Transactional
  public azhukov.model.User createUser(azhukov.model.UserCreateRequest request) {
    log.info("Creating new user: {}", request.getEmail());

    if (repository.existsByEmail(request.getEmail())) {
      throw new ValidationException("User with email " + request.getEmail() + " already exists");
    }

    UserEntity user = userMapper.toEntity(request);
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setActive(true);

    UserEntity savedUser = repository.save(user);
    return userMapper.toDto(savedUser);
  }

  /** Обновляет пользователя */
  @Transactional
  public azhukov.model.User updateUser(Long id, BaseUserFields request) {
    log.info("Updating user: {}", id);

    UserEntity user = findByIdOrThrow(id);
    userMapper.updateEntityFromRequest(request, user);

    UserEntity updatedUser = repository.save(user);
    return userMapper.toDto(updatedUser);
  }

  /** Получает пользователя по ID */
  @Transactional(readOnly = true)
  public azhukov.model.User getUserById(Long id) {
    log.debug("Getting user by id: {}", id);
    UserEntity user = findByIdOrThrow(id);
    return userMapper.toDto(user);
  }

  /** Получает пользователя по email */
  @Transactional(readOnly = true)
  public azhukov.model.User getUserByEmail(String email) {
    log.debug("Getting user by email: {}", email);
    UserEntity user =
        repository
            .findByEmail(email)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email));
    return userMapper.toDto(user);
  }

  /** Получает текущего пользователя по email */
  @Transactional(readOnly = true)
  public azhukov.model.User getCurrentUser(String email) {
    log.debug("Getting current user: {}", email);
    UserEntity entity =
        repository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    return userMapper.toDto(entity);
  }

  /** Обновляет текущего пользователя */
  @Transactional
  public azhukov.model.User updateCurrentUser(String email, BaseUserFields request) {
    log.debug("Updating current user: {}", email);

    UserEntity user =
        repository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

    userMapper.updateEntityFromRequest(request, user);

    UserEntity updatedUser = repository.save(user);
    return userMapper.toDto(updatedUser);
  }

  /** Получает всех пользователей */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> getAllUsers() {
    log.debug("Getting all users");
    List<UserEntity> users = repository.findAll();
    return userMapper.toDtoList(users);
  }

  /** Получает всех пользователей с пагинацией */
  @Transactional(readOnly = true)
  public Page<azhukov.model.User> getAllUsers(Pageable pageable) {
    log.debug("Getting all users with pagination");
    Page<UserEntity> users = repository.findAll(pageable);
    return userMapper.toDtoPage(users);
  }

  /** Получает всех пользователей с пагинацией (алиас для совместимости) */
  @Transactional(readOnly = true)
  public Page<azhukov.model.User> findAllUsers(Pageable pageable) {
    return getAllUsers(pageable);
  }

  /** Получает пользователя по ID (алиас для совместимости) */
  @Transactional(readOnly = true)
  public azhukov.model.User findUserById(Long id) {
    return getUserById(id);
  }

  /** Получает пользователя по email (алиас для совместимости) */
  @Transactional(readOnly = true)
  public azhukov.model.User findUserByEmail(String email) {
    return getUserByEmail(email);
  }

  /** Получает пользователей по роли */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> getUsersByRole(azhukov.model.RoleEnum role) {
    log.debug("Getting users by role: {}", role);
    UserEntity.Role entityRole = userMapper.mapRole(role);
    List<UserEntity> users = repository.findByRole(entityRole);
    return userMapper.toDtoList(users);
  }

  /** Получает пользователей по роли (алиас для совместимости) */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> findUsersByRole(UserEntity.Role role) {
    log.debug("Finding users by role: {}", role);
    List<UserEntity> users = repository.findByRole(role);
    return userMapper.toDtoList(users);
  }

  /** Получает активных пользователей */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> getActiveUsers() {
    log.debug("Getting active users");
    List<UserEntity> users = repository.findAllActive();
    return userMapper.toDtoList(users);
  }

  /** Ищет пользователей по поисковому запросу */
  @Transactional(readOnly = true)
  public Page<azhukov.model.User> searchUsers(String search, Pageable pageable) {
    log.debug("Searching users with term: {}", search);
    Page<UserEntity> users = repository.findBySearchTerm(search, pageable);
    return userMapper.toDtoPage(users);
  }

  /** Получает пользователей по языку */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> getUsersByLanguage(String language) {
    log.debug("Getting users by language: {}", language);
    List<UserEntity> users = repository.findByLanguage(language);
    return userMapper.toDtoList(users);
  }

  /** Получает пользователей по ID */
  @Transactional(readOnly = true)
  public List<azhukov.model.User> getUsersByIds(List<Long> ids) {
    log.debug("Getting users by ids: {}", ids);
    List<UserEntity> users = repository.findByIds(ids);
    return userMapper.toDtoList(users);
  }

  /** Мягко удаляет пользователя */
  @Transactional
  public void softDeleteUser(Long id) {
    log.info("Soft deleting user: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    repository.softDelete(id);
  }

  /** Восстанавливает пользователя */
  @Transactional
  public void restoreUser(Long id) {
    log.info("Restoring user: {}", id);
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    repository.restore(id);
  }

  /** Удаляет пользователя */
  @Transactional
  public void deleteUser(Long id) {
    log.debug("Deleting user with ID: {}", id);

    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }

    repository.deleteById(id);
    log.info("Deleted user with ID: {}", id);
  }

  /** Изменяет пароль пользователя */
  @Transactional
  public void changePassword(Long id, String newPassword) {
    log.info("Changing password for user: {}", id);
    UserEntity user = findByIdOrThrow(id);
    user.setPassword(passwordEncoder.encode(newPassword));
    repository.save(user);
  }

  /** Активирует пользователя */
  @Transactional
  public void activateUser(Long id) {
    log.info("Activating user: {}", id);
    UserEntity user = findByIdOrThrow(id);
    user.setActive(true);
    repository.save(user);
  }

  /** Деактивирует пользователя */
  @Transactional
  public void deactivateUser(Long id) {
    log.info("Deactivating user: {}", id);
    UserEntity user = findByIdOrThrow(id);
    user.setActive(false);
    repository.save(user);
  }

  /** Переключает статус пользователя */
  @Transactional
  public azhukov.model.User toggleUserStatus(Long id) {
    log.debug("Toggling status for user with ID: {}", id);

    UserEntity user = findByIdOrThrow(id);
    user.setActive(!user.isActive());
    UserEntity updatedUser = repository.save(user);

    log.info(
        "Toggled status for user with ID: {} to {}", updatedUser.getId(), updatedUser.isActive());
    return userMapper.toDto(updatedUser);
  }

  /** Подсчитывает количество пользователей по роли */
  @Transactional(readOnly = true)
  public long countUsersByRole(azhukov.model.RoleEnum role) {
    UserEntity.Role entityRole = userMapper.mapRole(role);
    return repository.countByRole(entityRole);
  }

  /** Подсчитывает количество активных пользователей */
  @Transactional(readOnly = true)
  public long countActiveUsers() {
    return repository.countActive();
  }

  /** Проверяет существование пользователя по email */
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return repository.existsByEmail(email);
  }

  @Transactional(readOnly = true)
  public UserEntity getUserEntityByEmail(String email) {
    return repository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
  }
}
