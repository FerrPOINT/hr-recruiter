package azhukov.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import azhukov.entity.Position;
import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.PositionMapper;
import azhukov.model.PositionCreateRequest;
import azhukov.model.PositionStatusEnum;
import azhukov.model.PositionUpdateRequest;
import azhukov.repository.PositionRepository;
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

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

  @Mock private PositionRepository positionRepository;

  @Mock private PositionMapper positionMapper;

  @Mock private UserRepository userRepository;

  @InjectMocks private PositionService positionService;

  private Position testPosition;
  private azhukov.model.Position testPositionDto;
  private UserEntity testUser;
  private PositionCreateRequest testCreateRequest;
  private PositionUpdateRequest testUpdateRequest;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");

    testPosition = new Position();
    testPosition.setId(1L);
    testPosition.setTitle("Java Developer");
    testPosition.setDescription("Senior Java Developer position");
    testPosition.setStatus(Position.Status.ACTIVE);
    testPosition.setCreatedBy(testUser);

    testPositionDto = new azhukov.model.Position();
    testPositionDto.setId(1L);
    testPositionDto.setTitle("Java Developer");
    testPositionDto.setDescription("Senior Java Developer position");
    testPositionDto.setStatus(PositionStatusEnum.ACTIVE);

    testCreateRequest = new PositionCreateRequest();
    testCreateRequest.setTitle("Java Developer");
    testCreateRequest.setDescription("Senior Java Developer position");

    testUpdateRequest = new PositionUpdateRequest();
    testUpdateRequest.setTitle("Senior Java Developer");
    testUpdateRequest.setDescription("Updated description");
  }

  @Test
  void createPosition_Success() {
    // Arrange
    String userEmail = "test@example.com";
    when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
    when(positionMapper.toEntity(testCreateRequest)).thenReturn(testPosition);
    when(positionRepository.save(any(Position.class))).thenReturn(testPosition);
    when(positionMapper.toDto(testPosition)).thenReturn(testPositionDto);

    // Act
    azhukov.model.Position result = positionService.createPosition(testCreateRequest, userEmail);

    // Assert
    assertNotNull(result);
    assertEquals(testPositionDto.getTitle(), result.getTitle());
    verify(userRepository).findByEmail(userEmail);
    verify(positionMapper).toEntity(testCreateRequest);
    verify(positionRepository).save(any(Position.class));
    verify(positionMapper).toDto(testPosition);
  }

  @Test
  void createPosition_UserNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    String userEmail = "nonexistent@example.com";
    when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> positionService.createPosition(testCreateRequest, userEmail));
    assertEquals("User not found with email: " + userEmail, exception.getMessage());
    verify(userRepository).findByEmail(userEmail);
    verify(positionRepository, never()).save(any());
  }

  @Test
  void getPositionById_Success() {
    // Arrange
    when(positionRepository.findByIdWithTopicsAndTeam(1L)).thenReturn(Optional.of(testPosition));
    when(positionMapper.toDto(testPosition)).thenReturn(testPositionDto);

    // Act
    azhukov.model.Position result = positionService.getPositionById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(testPositionDto.getId(), result.getId());
    verify(positionRepository).findByIdWithTopicsAndTeam(1L);
    verify(positionMapper).toDto(testPosition);
  }

  @Test
  void getPositionById_PositionNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(positionRepository.findByIdWithTopicsAndTeam(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> positionService.getPositionById(1L));
    assertEquals("Position not found with id: 1", exception.getMessage());
    verify(positionRepository).findByIdWithTopicsAndTeam(1L);
  }

  @Test
  void updatePosition_Success() {
    // Arrange
    when(positionRepository.findById(1L)).thenReturn(Optional.of(testPosition));
    when(positionRepository.save(any(Position.class))).thenReturn(testPosition);
    when(positionMapper.toDto(testPosition)).thenReturn(testPositionDto);

    // Act
    azhukov.model.Position result = positionService.updatePosition(1L, testUpdateRequest);

    // Assert
    assertNotNull(result);
    verify(positionRepository).findById(1L);
    verify(positionMapper).updateEntityFromRequest(testUpdateRequest, testPosition);
    verify(positionRepository).save(any(Position.class));
    verify(positionMapper).toDto(testPosition);
  }

  @Test
  void getAllPositions_Success() {
    // Arrange
    List<Position> positions = Arrays.asList(testPosition);
    List<azhukov.model.Position> positionDtos = Arrays.asList(testPositionDto);
    when(positionRepository.findAll()).thenReturn(positions);
    when(positionMapper.toDtoList(positions)).thenReturn(positionDtos);

    // Act
    List<azhukov.model.Position> result = positionService.getAllPositions();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testPositionDto.getTitle(), result.get(0).getTitle());
    verify(positionRepository).findAll();
    verify(positionMapper).toDtoList(positions);
  }

  @Test
  void getAllPositionsWithPagination_Success() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<Position> positionPage = new PageImpl<>(Arrays.asList(testPosition));
    Page<azhukov.model.Position> positionDtoPage = new PageImpl<>(Arrays.asList(testPositionDto));
    when(positionRepository.findAll(pageable)).thenReturn(positionPage);
    when(positionMapper.toDtoPage(positionPage)).thenReturn(positionDtoPage);

    // Act
    Page<azhukov.model.Position> result = positionService.getAllPositions(pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testPositionDto.getTitle(), result.getContent().get(0).getTitle());
    verify(positionRepository).findAll(pageable);
    verify(positionMapper).toDtoPage(positionPage);
  }

  @Test
  void searchPositions_Success() {
    // Arrange
    String searchTerm = "java";
    Pageable pageable = PageRequest.of(0, 10);
    Page<Position> positionPage = new PageImpl<>(Arrays.asList(testPosition));
    Page<azhukov.model.Position> positionDtoPage = new PageImpl<>(Arrays.asList(testPositionDto));
    when(positionRepository.findBySearchTerm(searchTerm, pageable)).thenReturn(positionPage);
    when(positionMapper.toDtoPage(positionPage)).thenReturn(positionDtoPage);

    // Act
    Page<azhukov.model.Position> result = positionService.searchPositions(searchTerm, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(positionRepository).findBySearchTerm(searchTerm, pageable);
    verify(positionMapper).toDtoPage(positionPage);
  }

  @Test
  void getPositionsByStatus_Success() {
    // Arrange
    PositionStatusEnum status = PositionStatusEnum.ACTIVE;
    List<Position> positions = Arrays.asList(testPosition);
    List<azhukov.model.Position> positionDtos = Arrays.asList(testPositionDto);
    when(positionMapper.mapStatus(status)).thenReturn(Position.Status.ACTIVE);
    when(positionRepository.findByStatus(Position.Status.ACTIVE)).thenReturn(positions);
    when(positionMapper.toDtoList(positions)).thenReturn(positionDtos);

    // Act
    List<azhukov.model.Position> result = positionService.getPositionsByStatus(status);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(positionMapper).mapStatus(status);
    verify(positionRepository).findByStatus(Position.Status.ACTIVE);
    verify(positionMapper).toDtoList(positions);
  }

  @Test
  void softDeletePosition_Success() {
    // Arrange
    when(positionRepository.existsById(1L)).thenReturn(true);
    when(positionRepository.findById(1L)).thenReturn(Optional.of(testPosition));
    when(positionRepository.save(any(Position.class))).thenReturn(testPosition);

    // Act
    positionService.softDeletePosition(1L);

    // Assert
    verify(positionRepository).existsById(1L);
    verify(positionRepository).findById(1L);
    verify(positionRepository).save(any(Position.class));
  }

  @Test
  void softDeletePosition_PositionNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(positionRepository.existsById(1L)).thenReturn(false);

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> positionService.softDeletePosition(1L));
    assertEquals("Position not found with id: 1", exception.getMessage());
    verify(positionRepository).existsById(1L);
    verify(positionRepository, never()).softDelete(any());
  }

  @Test
  void updatePositionStatus_Success() {
    // Arrange
    PositionStatusEnum newStatus = PositionStatusEnum.ARCHIVED;
    when(positionRepository.findById(1L)).thenReturn(Optional.of(testPosition));
    when(positionMapper.mapStatus(newStatus)).thenReturn(Position.Status.ARCHIVED);
    when(positionRepository.save(any(Position.class))).thenReturn(testPosition);
    when(positionMapper.toDto(testPosition)).thenReturn(testPositionDto);

    // Act
    azhukov.model.Position result = positionService.updatePositionStatus(1L, newStatus);

    // Assert
    assertNotNull(result);
    verify(positionRepository).findById(1L);
    verify(positionMapper).mapStatus(newStatus);
    verify(positionRepository).save(any(Position.class));
    verify(positionMapper).toDto(testPosition);
  }

  @Test
  void countPositionsByStatus_Success() {
    // Arrange
    PositionStatusEnum status = PositionStatusEnum.ACTIVE;
    when(positionMapper.mapStatus(status)).thenReturn(Position.Status.ACTIVE);
    when(positionRepository.countByStatus(Position.Status.ACTIVE)).thenReturn(5L);

    // Act
    long result = positionService.countPositionsByStatus(status);

    // Assert
    assertEquals(5L, result);
    verify(positionMapper).mapStatus(status);
    verify(positionRepository).countByStatus(Position.Status.ACTIVE);
  }
}
