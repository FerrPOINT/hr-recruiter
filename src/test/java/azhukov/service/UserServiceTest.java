package azhukov.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.mapper.UserMapper;
import azhukov.model.RoleEnum;
import azhukov.model.User;
import azhukov.model.UserCreateRequest;
import azhukov.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private UserMapper userMapper;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private UserEntity testUserEntity;
  private User testUser;
  private UserCreateRequest testCreateRequest;

  @BeforeEach
  void setUp() {
    testUserEntity = new UserEntity();
    testUserEntity.setId(1L);
    testUserEntity.setEmail("test@example.com");
    testUserEntity.setFirstName("John");
    testUserEntity.setLastName("Doe");
    testUserEntity.setRole(UserEntity.Role.RECRUITER);
    testUserEntity.setActive(true);

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setRole(RoleEnum.RECRUITER);

    testCreateRequest = new UserCreateRequest();
    testCreateRequest.setEmail("new@example.com");
    testCreateRequest.setPassword("password123");
    testCreateRequest.setFirstName("Jane");
    testCreateRequest.setLastName("Smith");
    testCreateRequest.setRole(RoleEnum.RECRUITER);
  }

  @Test
  void createUser_Success() {
    // Arrange
    when(userRepository.existsByEmail(testCreateRequest.getEmail())).thenReturn(false);
    when(userMapper.toEntity(testCreateRequest)).thenReturn(testUserEntity);
    when(passwordEncoder.encode(testCreateRequest.getPassword())).thenReturn("encodedPassword");
    when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);
    when(userMapper.toDto(testUserEntity)).thenReturn(testUser);

    // Act
    User result = userService.createUser(testCreateRequest);

    // Assert
    assertNotNull(result);
    assertEquals(testUser.getEmail(), result.getEmail());
    verify(userRepository).existsByEmail(testCreateRequest.getEmail());
    verify(userMapper).toEntity(testCreateRequest);
    verify(passwordEncoder).encode(testCreateRequest.getPassword());
    verify(userRepository).save(any(UserEntity.class));
    verify(userMapper).toDto(testUserEntity);
  }

  @Test
  void createUser_EmailAlreadyExists_ThrowsValidationException() {
    // Arrange
    when(userRepository.existsByEmail(testCreateRequest.getEmail())).thenReturn(true);

    // Act & Assert
    ValidationException exception =
        assertThrows(ValidationException.class, () -> userService.createUser(testCreateRequest));
    assertEquals(
        "User with email " + testCreateRequest.getEmail() + " already exists",
        exception.getMessage());
    verify(userRepository).existsByEmail(testCreateRequest.getEmail());
    verify(userRepository, never()).save(any());
  }

  @Test
  void getUserById_Success() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUserEntity));
    when(userMapper.toDto(testUserEntity)).thenReturn(testUser);

    // Act
    User result = userService.getUserById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(testUser.getId(), result.getId());
    verify(userRepository).findById(1L);
    verify(userMapper).toDto(testUserEntity);
  }

  @Test
  void getUserById_UserNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
    assertEquals("Not found: 1", exception.getMessage());
    verify(userRepository).findById(1L);
  }

  @Test
  void getUserByEmail_Success() {
    // Arrange
    String email = "test@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUserEntity));
    when(userMapper.toDto(testUserEntity)).thenReturn(testUser);

    // Act
    User result = userService.getUserByEmail(email);

    // Assert
    assertNotNull(result);
    assertEquals(testUser.getEmail(), result.getEmail());
    verify(userRepository).findByEmail(email);
    verify(userMapper).toDto(testUserEntity);
  }

  @Test
  void getUserByEmail_UserNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    String email = "nonexistent@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail(email));
    assertEquals("User not found with email: " + email, exception.getMessage());
    verify(userRepository).findByEmail(email);
  }

  @Test
  void getAllUsers_Success() {
    // Arrange
    List<UserEntity> userEntities = Arrays.asList(testUserEntity);
    List<User> users = Arrays.asList(testUser);
    when(userRepository.findAll()).thenReturn(userEntities);
    when(userMapper.toDtoList(userEntities)).thenReturn(users);

    // Act
    List<User> result = userService.getAllUsers();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testUser.getEmail(), result.get(0).getEmail());
    verify(userRepository).findAll();
    verify(userMapper).toDtoList(userEntities);
  }

  @Test
  void getAllUsersWithPagination_Success() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<UserEntity> userEntityPage = new PageImpl<>(Arrays.asList(testUserEntity));
    Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
    when(userRepository.findAll(pageable)).thenReturn(userEntityPage);
    when(userMapper.toDtoPage(userEntityPage)).thenReturn(userPage);

    // Act
    Page<User> result = userService.getAllUsers(pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testUser.getEmail(), result.getContent().get(0).getEmail());
    verify(userRepository).findAll(pageable);
    verify(userMapper).toDtoPage(userEntityPage);
  }

  @Test
  void searchUsers_Success() {
    // Arrange
    String searchTerm = "john";
    Pageable pageable = PageRequest.of(0, 10);
    Page<UserEntity> userEntityPage = new PageImpl<>(Arrays.asList(testUserEntity));
    Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
    when(userRepository.findBySearchTerm(searchTerm, pageable)).thenReturn(userEntityPage);
    when(userMapper.toDtoPage(userEntityPage)).thenReturn(userPage);

    // Act
    Page<User> result = userService.searchUsers(searchTerm, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(userRepository).findBySearchTerm(searchTerm, pageable);
    verify(userMapper).toDtoPage(userEntityPage);
  }

  @Test
  void softDeleteUser_Success() {
    // Arrange
    when(userRepository.existsById(1L)).thenReturn(true);

    // Act
    userService.softDeleteUser(1L);

    // Assert
    verify(userRepository).existsById(1L);
    verify(userRepository).softDelete(1L);
  }

  @Test
  void softDeleteUser_UserNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(userRepository.existsById(1L)).thenReturn(false);

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.softDeleteUser(1L));
    assertEquals("User not found with id: 1", exception.getMessage());
    verify(userRepository).existsById(1L);
    verify(userRepository, never()).softDelete(any());
  }

  @Test
  void getUsersByRole_Success() {
    // Arrange
    RoleEnum role = RoleEnum.RECRUITER;
    List<UserEntity> userEntities = Arrays.asList(testUserEntity);
    List<User> users = Arrays.asList(testUser);
    when(userMapper.mapRole(role)).thenReturn(UserEntity.Role.RECRUITER);
    when(userRepository.findByRole(UserEntity.Role.RECRUITER)).thenReturn(userEntities);
    when(userMapper.toDtoList(userEntities)).thenReturn(users);

    // Act
    List<User> result = userService.getUsersByRole(role);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(userMapper).mapRole(role);
    verify(userRepository).findByRole(UserEntity.Role.RECRUITER);
    verify(userMapper).toDtoList(userEntities);
  }

  @Test
  void existsByEmail_Success() {
    // Arrange
    String email = "test@example.com";
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // Act
    boolean result = userService.existsByEmail(email);

    // Assert
    assertTrue(result);
    verify(userRepository).existsByEmail(email);
  }

  @Test
  void countActiveUsers_Success() {
    // Arrange
    when(userRepository.countActive()).thenReturn(5L);

    // Act
    long result = userService.countActiveUsers();

    // Assert
    assertEquals(5L, result);
    verify(userRepository).countActive();
  }
}
