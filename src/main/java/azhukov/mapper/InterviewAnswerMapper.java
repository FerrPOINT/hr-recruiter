package azhukov.mapper;

import azhukov.entity.InterviewAnswer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер для преобразования между сущностью InterviewAnswer и DTO. Использует MapStruct для
 * автоматической генерации кода.
 */
@Mapper(componentModel = "spring")
public interface InterviewAnswerMapper {

  /** Преобразует InterviewAnswer entity в InterviewAnswer DTO */
  @Mapping(target = "id", source = "id")
  @Mapping(target = "interviewId", source = "interview.id")
  @Mapping(target = "questionId", source = "question.id")
  @Mapping(target = "answerText", source = "answerText")
  @Mapping(target = "audioUrl", source = "audioUrl")
  @Mapping(target = "transcript", source = "transcript")
  @Mapping(target = "createdAt", qualifiedByName = "answerLocalDateTimeToOffsetDateTime")
  azhukov.model.InterviewAnswer toDto(InterviewAnswer interviewAnswer);

  /** Преобразует InterviewAnswerCreateRequest в InterviewAnswer entity */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "interview", ignore = true) // Устанавливается в сервисе
  @Mapping(target = "question", ignore = true) // Устанавливается в сервисе
  @Mapping(target = "answerText", source = "answerText")
  @Mapping(target = "audioUrl", source = "audioUrl")
  @Mapping(target = "transcript", source = "transcript")
  @Mapping(target = "createdAt", ignore = true)
  InterviewAnswer toEntity(azhukov.model.InterviewAnswerCreateRequest request);

  /** Преобразует список InterviewAnswer в список InterviewAnswerDto */
  List<azhukov.model.InterviewAnswer> toDtoList(List<InterviewAnswer> entities);

  /** Преобразует LocalDateTime в OffsetDateTime для InterviewAnswer */
  @Named("answerLocalDateTimeToOffsetDateTime")
  default OffsetDateTime answerLocalDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
    if (localDateTime == null) return null;
    return localDateTime.atOffset(ZoneOffset.UTC);
  }

  /** Преобразует OffsetDateTime в LocalDateTime */
  @Named("offsetDateTimeToLocalDateTime")
  default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) return null;
    return offsetDateTime.toLocalDateTime();
  }
}
