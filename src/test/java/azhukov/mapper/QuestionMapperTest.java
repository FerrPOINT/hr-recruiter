package azhukov.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import azhukov.entity.Question;
import azhukov.model.BaseQuestionFields;
import azhukov.model.QuestionCreateRequest;
import azhukov.model.QuestionTypeEnum;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionMapperTest {

  private QuestionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(QuestionMapper.class);
  }

  @Test
  void toDto_mapsEntityToDto() {
    Question entity =
        Question.builder()
            .id(1L)
            .text("What is Java?")
            .type(Question.Type.TEXT)
            .order(1)
            .required(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    var dto = mapper.toDto(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getText()).isEqualTo("What is Java?");
    assertThat(dto.getType()).isEqualTo(QuestionTypeEnum.TEXT);
    assertThat(dto.getOrder()).isEqualTo(1);
    assertThat(dto.getIsRequired()).isTrue();
  }

  @Test
  void toEntity_mapsCreateRequestToEntity() {
    QuestionCreateRequest req =
        new QuestionCreateRequest()
            .text("What is OOP?")
            .type(QuestionTypeEnum.TEXT)
            .order(2L)
            .isRequired(false);

    Question entity = mapper.toEntity(req);
    assertThat(entity).isNotNull();
    assertThat(entity.getText()).isEqualTo("What is OOP?");
    assertThat(entity.getType()).isEqualTo(Question.Type.TEXT);
    assertThat(entity.getOrder()).isEqualTo(2L);
    assertThat(entity.isRequired()).isFalse();
  }

  @Test
  void toEntity_mapsBaseQuestionFieldsToEntity() {
    BaseQuestionFields base =
        new BaseQuestionFields()
            .text("Explain polymorphism")
            .type(QuestionTypeEnum.TEXT)
            .order(3L)
            .isRequired(true);

    Question entity = mapper.toEntity(base);
    assertThat(entity).isNotNull();
    assertThat(entity.getText()).isEqualTo("Explain polymorphism");
    assertThat(entity.getType()).isEqualTo(Question.Type.TEXT);
    assertThat(entity.getOrder()).isEqualTo(3L);
    assertThat(entity.isRequired()).isTrue();
  }
}
