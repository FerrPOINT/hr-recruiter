package azhukov.mapper;

import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.model.BaseQuestionFields;
import azhukov.model.QuestionCreateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.*;

/**
 * Маппер для преобразования между Question entity и DTO. Использует MapStruct для автоматической
 * генерации кода маппинга.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionMapper extends CommonMapper {

  /** Преобразует Question entity в Question DTO. */
  @Mapping(target = "id", source = "id")
  @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(entity.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(entity.getUpdatedAt()))")
  @Mapping(target = "text", source = "text")
  @Mapping(target = "type", expression = "java(toQuestionTypeEnum(entity.getType()))")
  @Mapping(target = "order", source = "order")
  @Mapping(target = "isRequired", source = "required")
  @Mapping(target = "maxDuration", source = "maxDuration")
  @Mapping(target = "options", source = "options")
  @Mapping(target = "evaluationCriteria", ignore = true) // Поле отсутствует в entity
  @Mapping(
      target = "positionId",
      expression = "java(entity.getPosition() != null ? entity.getPosition().getId() : null)")
  azhukov.model.Question toDto(Question entity);

  /** Преобразует QuestionCreateRequest в Question entity. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "position", ignore = true) // Устанавливается в сервисе
  @Mapping(target = "text", source = "text")
  @Mapping(target = "type", expression = "java(toQuestionType(request.getType()))")
  @Mapping(target = "order", source = "order")
  @Mapping(target = "required", source = "isRequired")
  @Mapping(target = "maxDuration", ignore = true)
  @Mapping(target = "options", ignore = true)
  @Mapping(target = "answers", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Question toEntity(QuestionCreateRequest request);

  /** Преобразует BaseQuestionFields в Question entity. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "position", ignore = true)
  @Mapping(target = "text", source = "text")
  @Mapping(target = "type", expression = "java(toQuestionType(entity.getType()))")
  @Mapping(target = "order", source = "order")
  @Mapping(target = "required", source = "isRequired")
  @Mapping(target = "maxDuration", ignore = true)
  @Mapping(target = "options", ignore = true)
  @Mapping(target = "answers", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Question toEntity(BaseQuestionFields entity);

  /** Обновляет Question entity из BaseQuestionFields. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "position", ignore = true)
  @Mapping(target = "text", source = "text")
  @Mapping(target = "type", expression = "java(toQuestionType(entity.getType()))")
  @Mapping(target = "order", source = "order")
  @Mapping(target = "required", source = "isRequired")
  @Mapping(target = "maxDuration", ignore = true)
  @Mapping(target = "options", ignore = true)
  @Mapping(target = "answers", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(BaseQuestionFields entity, @MappingTarget Question question);

  /**
   * Преобразует список сущностей в список DTO.
   *
   * @param questions Список сущностей
   * @return Список DTO
   */
  List<azhukov.model.Question> toDtoList(List<Question> questions);

  /** Преобразует Position в PositionQuestionsResponseInterviewSettings */
  @Mapping(
      target = "answerTime",
      expression =
          "java(position.getAnswerTime() != null ? position.getAnswerTime().longValue() : null)")
  @Mapping(target = "language", source = "language")
  @Mapping(target = "showOtherLang", source = "showOtherLang")
  @Mapping(target = "saveAudio", source = "saveAudio")
  @Mapping(target = "saveVideo", source = "saveVideo")
  @Mapping(target = "randomOrder", source = "randomOrder")
  @Mapping(target = "questionType", source = "questionType")
  @Mapping(
      target = "questionsCount",
      expression =
          "java(position.getQuestionsCount() != null ? position.getQuestionsCount().longValue() : null)")
  @Mapping(target = "checkType", source = "checkType")
  @Mapping(
      target = "level",
      expression =
          "java(position.getLevel() != null ? position.getLevel().name().toLowerCase() : null)")
  azhukov.model.PositionQuestionsResponseInterviewSettings toInterviewSettings(Position position);

  /** Маппинг типов вопросов */
  @ValueMapping(source = "TEXT", target = "TEXT")
  @ValueMapping(source = "AUDIO", target = "AUDIO")
  @ValueMapping(source = "VIDEO", target = "VIDEO")
  @ValueMapping(source = "CHOICE", target = "CHOICE")
  azhukov.model.QuestionTypeEnum mapType(Question.Type type);

  /** Обратный маппинг типов вопросов */
  @ValueMapping(source = "TEXT", target = "TEXT")
  @ValueMapping(source = "AUDIO", target = "AUDIO")
  @ValueMapping(source = "VIDEO", target = "VIDEO")
  @ValueMapping(source = "CHOICE", target = "CHOICE")
  Question.Type mapType(azhukov.model.QuestionTypeEnum type);

  // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

  /** Преобразует LocalDateTime в OffsetDateTime. */
  default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
    return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
  }

  /** Преобразует Question.Type в QuestionTypeEnum. */
  default azhukov.model.QuestionTypeEnum toQuestionTypeEnum(Question.Type type) {
    if (type == null) return null;
    return switch (type) {
      case TEXT -> azhukov.model.QuestionTypeEnum.TEXT;
      case AUDIO -> azhukov.model.QuestionTypeEnum.AUDIO;
      case VIDEO -> azhukov.model.QuestionTypeEnum.VIDEO;
      case CHOICE -> azhukov.model.QuestionTypeEnum.CHOICE;
    };
  }

  /** Преобразует QuestionTypeEnum в Question.Type. */
  default Question.Type toQuestionType(azhukov.model.QuestionTypeEnum type) {
    if (type == null) return null;
    return switch (type) {
      case TEXT -> Question.Type.TEXT;
      case AUDIO -> Question.Type.AUDIO;
      case VIDEO -> Question.Type.VIDEO;
      case CHOICE -> Question.Type.CHOICE;
    };
  }
}
