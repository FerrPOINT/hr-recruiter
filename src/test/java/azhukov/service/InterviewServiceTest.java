package azhukov.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import azhukov.entity.Candidate;
import azhukov.entity.Interview;
import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.mapper.CandidateMapper;
import azhukov.mapper.InterviewMapper;
import azhukov.mapper.PositionMapper;
import azhukov.mapper.QuestionMapper;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import azhukov.repository.QuestionRepository;
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
class InterviewServiceTest {

  @Mock private InterviewRepository interviewRepository;

  @Mock private CandidateRepository candidateRepository;

  @Mock private PositionRepository positionRepository;

  @Mock private QuestionRepository questionRepository;

  @Mock private InterviewAnswerRepository interviewAnswerRepository;

  @Mock private InterviewMapper interviewMapper;

  @Mock private CandidateMapper candidateMapper;

  @Mock private PositionMapper positionMapper;

  @Mock private QuestionMapper questionMapper;

  @InjectMocks private InterviewService interviewService;

  private Interview testInterview;
  private Candidate testCandidate;
  private Position testPosition;
  private Question testQuestion;

  @BeforeEach
  void setUp() {
    testPosition = new Position();
    testPosition.setId(1L);
    testPosition.setTitle("Java Developer");
    testPosition.setQuestionsCount(5);

    testCandidate = spy(new Candidate());
    testCandidate.setId(1L);
    testCandidate.setFirstName("John");
    testCandidate.setLastName("Doe");
    testCandidate.setPosition(testPosition);

    testQuestion = new Question();
    testQuestion.setId(1L);
    testQuestion.setPosition(testPosition);
    testQuestion.setText("What is Java?");

    testInterview =
        Interview.builder()
            .id(1L)
            .candidate(testCandidate)
            .position(testPosition)
            .status(Interview.Status.NOT_STARTED)
            .build();
  }

  @Test
  void createInterviewFromCandidate_Success() {
    // Arrange
    when(candidateRepository.findById(1L)).thenReturn(Optional.of(testCandidate));
    when(testCandidate.hasActiveInterview()).thenReturn(false);
    when(interviewRepository.save(any(Interview.class))).thenReturn(testInterview);
    when(interviewMapper.toDto(any(Interview.class))).thenReturn(new azhukov.model.Interview());

    // Act
    azhukov.model.Interview result = interviewService.createInterviewFromCandidate(1L);

    // Assert
    assertNotNull(result);
    verify(candidateRepository).findById(1L);
    verify(testCandidate).hasActiveInterview();
    verify(interviewRepository).save(any(Interview.class));
  }

  @Test
  void createInterviewFromCandidate_CandidateNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(candidateRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> interviewService.createInterviewFromCandidate(1L));
    assertEquals("Кандидат не найден: 1", exception.getMessage());
    verify(candidateRepository).findById(1L);
    verify(interviewRepository, never()).save(any());
  }

  @Test
  void createInterviewFromCandidate_AlreadyHasActiveInterview_ThrowsValidationException() {
    // Arrange
    when(candidateRepository.findById(1L)).thenReturn(Optional.of(testCandidate));
    when(testCandidate.hasActiveInterview()).thenReturn(true);

    // Act & Assert
    ValidationException exception =
        assertThrows(
            ValidationException.class, () -> interviewService.createInterviewFromCandidate(1L));
    assertEquals("У кандидата уже есть активное собеседование", exception.getMessage());
    verify(candidateRepository).findById(1L);
    verify(testCandidate).hasActiveInterview();
    verify(interviewRepository, never()).save(any());
  }

  @Test
  void finishInterview_Success() {
    // Arrange
    testInterview.setStatus(Interview.Status.IN_PROGRESS);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));
    when(interviewRepository.save(any(Interview.class))).thenReturn(testInterview);

    // Act
    Interview result = interviewService.finishInterview(1L);

    // Assert
    assertNotNull(result);
    verify(interviewRepository).findById(1L);
    verify(interviewRepository).save(testInterview);
    assertEquals(Interview.Status.FINISHED, testInterview.getStatus());
    assertNotNull(testInterview.getFinishedAt());
  }

  @Test
  void finishInterview_NotInProgress_ThrowsValidationException() {
    // Arrange
    testInterview.setStatus(Interview.Status.NOT_STARTED);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));

    // Act & Assert
    ValidationException exception =
        assertThrows(ValidationException.class, () -> interviewService.finishInterview(1L));
    assertEquals("Собеседование не может быть завершено в текущем статусе", exception.getMessage());
    verify(interviewRepository).findById(1L);
    verify(interviewRepository, never()).save(any());
  }

  @Test
  void submitInterviewAnswer_Success() {
    // Arrange
    testInterview.setStatus(Interview.Status.IN_PROGRESS);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));
    when(questionRepository.findById(1L)).thenReturn(Optional.of(testQuestion));
    when(interviewRepository.save(any(Interview.class))).thenReturn(testInterview);

    // Act
    Interview result =
        interviewService.submitInterviewAnswer(
            1L, 1L, "Java is a programming language", "audio.mp3", "transcript");

    // Assert
    assertNotNull(result);
    verify(interviewRepository).findById(1L);
    verify(questionRepository).findById(1L);
    verify(interviewRepository).save(testInterview);
    assertEquals(1, testInterview.getAnswers().size());
  }

  @Test
  void submitInterviewAnswer_InterviewNotInProgress_ThrowsValidationException() {
    // Arrange
    testInterview.setStatus(Interview.Status.FINISHED);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));

    // Act & Assert
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () ->
                interviewService.submitInterviewAnswer(
                    1L, 1L, "answer", "audio.mp3", "transcript"));
    assertEquals("Ответ можно добавить только к активному собеседованию", exception.getMessage());
    verify(interviewRepository).findById(1L);
    verify(questionRepository, never()).findById(any());
  }

  @Test
  void submitInterviewAnswer_QuestionNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    testInterview.setStatus(Interview.Status.IN_PROGRESS);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));
    when(questionRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () ->
                interviewService.submitInterviewAnswer(
                    1L, 1L, "answer", "audio.mp3", "transcript"));
    assertEquals("Вопрос не найден: 1", exception.getMessage());
    verify(interviewRepository).findById(1L);
    verify(questionRepository).findById(1L);
  }

  @Test
  void listInterviews_Success() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<Interview> interviewPage = new PageImpl<>(Arrays.asList(testInterview));
    when(interviewRepository.findAll(pageable)).thenReturn(interviewPage);

    // Act
    Page<Interview> result = interviewService.listInterviews(null, null, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(interviewRepository).findAll(pageable);
  }

  @Test
  void listInterviews_ByPosition_Success() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<Interview> interviewPage = new PageImpl<>(Arrays.asList(testInterview));
    when(interviewRepository.findByPositionId(1L, pageable)).thenReturn(interviewPage);

    // Act
    Page<Interview> result = interviewService.listInterviews(1L, null, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(interviewRepository).findByPositionId(1L, pageable);
  }

  @Test
  void listInterviews_ByCandidate_Success() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<Interview> interviewPage = new PageImpl<>(Arrays.asList(testInterview));
    when(interviewRepository.findByCandidateId(1L, pageable)).thenReturn(interviewPage);

    // Act
    Page<Interview> result = interviewService.listInterviews(null, 1L, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(interviewRepository).findByCandidateId(1L, pageable);
  }

  @Test
  void getInterviewsByPosition_Success() {
    // Arrange
    testPosition.setInterviews(Arrays.asList(testInterview));
    when(positionRepository.findById(1L)).thenReturn(Optional.of(testPosition));
    when(interviewMapper.toDtoList(org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(Arrays.asList(new azhukov.model.Interview()));

    // Act
    List<azhukov.model.Interview> result = interviewService.getInterviewsByPosition(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(positionRepository).findById(1L);
  }

  @Test
  void getInterviewsByPosition_PositionNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(positionRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> interviewService.getInterviewsByPosition(1L));
    assertEquals("Вакансия не найдена: 1", exception.getMessage());
    verify(positionRepository).findById(1L);
  }

  @Test
  void getInterviewsByCandidate_Success() {
    // Arrange
    List<Interview> interviews = Arrays.asList(testInterview);
    when(candidateRepository.findById(1L)).thenReturn(Optional.of(testCandidate));
    when(interviewRepository.findByCandidate(testCandidate)).thenReturn(interviews);

    // Act
    List<Interview> result = interviewService.getInterviewsByCandidate(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(candidateRepository).findById(1L);
    verify(interviewRepository).findByCandidate(testCandidate);
  }

  @Test
  void getInterviewsByCandidate_CandidateNotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(candidateRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> interviewService.getInterviewsByCandidate(1L));
    assertEquals("Кандидат не найден: 1", exception.getMessage());
    verify(candidateRepository).findById(1L);
  }

  @Test
  void getActiveInterviews_Success() {
    // Arrange
    List<Interview> interviews = Arrays.asList(testInterview);
    when(interviewRepository.findActiveInterviews()).thenReturn(interviews);

    // Act
    List<Interview> result = interviewService.getActiveInterviews();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(interviewRepository).findActiveInterviews();
  }

  @Test
  void getFinishedInterviews_Success() {
    // Arrange
    List<Interview> interviews = Arrays.asList(testInterview);
    when(interviewRepository.findFinishedInterviews()).thenReturn(interviews);

    // Act
    List<Interview> result = interviewService.getFinishedInterviews();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(interviewRepository).findFinishedInterviews();
  }

  @Test
  void getSuccessfulInterviews_Success() {
    // Arrange
    List<Interview> interviews = Arrays.asList(testInterview);
    when(interviewRepository.findSuccessfulInterviews()).thenReturn(interviews);

    // Act
    List<Interview> result = interviewService.getSuccessfulInterviews();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(interviewRepository).findSuccessfulInterviews();
  }

  @Test
  void submitInterviewAnswer_InterviewNotStarted_Success() {
    // Arrange
    testInterview.setStatus(Interview.Status.NOT_STARTED);
    when(interviewRepository.findById(1L)).thenReturn(Optional.of(testInterview));
    when(questionRepository.findById(1L)).thenReturn(Optional.of(testQuestion));
    when(interviewRepository.save(any(Interview.class))).thenReturn(testInterview);

    // Act
    Interview result =
        interviewService.submitInterviewAnswer(
            1L, 1L, "Java is a programming language", "audio.mp3", "transcript");

    // Assert
    assertNotNull(result);
    verify(interviewRepository).findById(1L);
    verify(questionRepository).findById(1L);
    verify(interviewRepository).save(testInterview);
    assertEquals(1, testInterview.getAnswers().size());
  }
}
