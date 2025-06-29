package azhukov.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.QuestionMapper;
import azhukov.repository.PositionRepository;
import azhukov.repository.QuestionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

  @Mock private QuestionRepository questionRepository;
  @Mock private QuestionMapper questionMapper;
  @Mock private PositionRepository positionRepository;

  @InjectMocks private QuestionService questionService;

  private Position testPosition;
  private Question testQuestion1;
  private Question testQuestion2;
  private azhukov.model.Question testModelQuestion1;
  private azhukov.model.Question testModelQuestion2;

  @BeforeEach
  void setUp() {
    // Создаем тестовую вакансию с настройками
    testPosition =
        Position.builder()
            .id(1L)
            .title("Java Developer")
            .answerTime(120)
            .language("ru")
            .showOtherLang(false)
            .saveAudio(true)
            .saveVideo(false)
            .randomOrder(false)
            .questionType("mixed")
            .questionsCount(10)
            .checkType("ai")
            .level(Position.Level.MIDDLE)
            .build();

    // Создаем тестовые вопросы
    testQuestion1 =
        Question.builder().id(1L).text("Расскажите о Spring Framework").order(1).build();

    testQuestion2 =
        Question.builder().id(2L).text("Что такое Dependency Injection?").order(2).build();

    // Создаем модели вопросов
    testModelQuestion1 = new azhukov.model.Question();
    testModelQuestion1.setId(1L);
    testModelQuestion1.setText("Расскажите о Spring Framework");

    testModelQuestion2 = new azhukov.model.Question();
    testModelQuestion2.setId(2L);
    testModelQuestion2.setText("Что такое Dependency Injection?");
  }

  @Test
  void getQuestionsWithSettingsByPosition_ShouldReturnQuestions() {
    // Given
    Long positionId = 1L;
    Position position = new Position();
    position.setId(positionId);

    Question question1 = new Question();
    question1.setId(1L);
    question1.setText("Question 1");
    question1.setPosition(position);

    Question question2 = new Question();
    question2.setId(2L);
    question2.setText("Question 2");
    question2.setPosition(position);

    List<Question> questions = List.of(question1, question2);

    when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));
    when(questionRepository.findByPositionOrderByOrderAsc(position)).thenReturn(questions);

    // When
    List<Question> result = questionService.getPositionQuestions(positionId);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getText()).isEqualTo("Question 1");
    assertThat(result.get(1).getText()).isEqualTo("Question 2");

    verify(positionRepository).findById(positionId);
    verify(questionRepository).findByPositionOrderByOrderAsc(position);
  }

  @Test
  void getPositionQuestions_PositionNotFound() {
    // Arrange
    Long positionId = 999L;
    when(positionRepository.findById(positionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          questionService.getPositionQuestions(positionId);
        });

    verify(positionRepository).findById(positionId);
    verify(questionRepository, never()).findByPositionOrderByOrderAsc(any());
  }
}
