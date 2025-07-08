package azhukov.mapper;

import azhukov.entity.Interview;
import azhukov.model.InterviewResultEnum;
import azhukov.model.InterviewStatusEnum;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер для преобразования между сущностью Interview и DTO. Использует MapStruct для
 * автоматической генерации кода.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InterviewMapper extends CommonMapper {

  /** Преобразует сущность Interview в DTO */
  @Mapping(source = "candidate.id", target = "candidateId")
  @Mapping(source = "position.id", target = "positionId")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "result", target = "result")
  @Mapping(source = "answers", target = "answers")
  @Mapping(source = "aiScore", target = "aiScore")
  @Mapping(
      source = "createdAt",
      target = "createdAt",
      qualifiedByName = "interviewLocalDateTimeToOffsetDateTime")
  @Mapping(
      source = "startedAt",
      target = "startedAt",
      qualifiedByName = "interviewLocalDateTimeToOffsetDateTime")
  @Mapping(
      source = "finishedAt",
      target = "finishedAt",
      qualifiedByName = "interviewLocalDateTimeToOffsetDateTime")
  azhukov.model.Interview toDto(Interview interview);

  /** Преобразует список сущностей Interview в список DTO */
  List<azhukov.model.Interview> toDtoList(List<Interview> interviews);

  /** Преобразует DTO в сущность Interview */
  @Mapping(target = "candidate", ignore = true)
  @Mapping(target = "position", ignore = true)
  @Mapping(target = "answers", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "startedAt", ignore = true)
  @Mapping(target = "finishedAt", ignore = true)
  Interview toEntity(azhukov.model.Interview interviewDto);

  /** Преобразует статус собеседования */
  @Named("statusToDto")
  default InterviewStatusEnum statusToDto(Interview.Status status) {
    if (status == null) return null;
    return switch (status) {
      case NOT_STARTED -> InterviewStatusEnum.NOT_STARTED;
      case IN_PROGRESS -> InterviewStatusEnum.IN_PROGRESS;
      case FINISHED -> InterviewStatusEnum.FINISHED;
    };
  }

  /** Преобразует статус собеседования из DTO */
  @Named("statusToEntity")
  default Interview.Status statusToEntity(InterviewStatusEnum status) {
    if (status == null) return null;
    return switch (status) {
      case NOT_STARTED -> Interview.Status.NOT_STARTED;
      case IN_PROGRESS -> Interview.Status.IN_PROGRESS;
      case FINISHED -> Interview.Status.FINISHED;
    };
  }

  /** Преобразует результат собеседования */
  @Named("resultToDto")
  default InterviewResultEnum resultToDto(Interview.Result result) {
    if (result == null) return null;
    return switch (result) {
      case SUCCESSFUL -> InterviewResultEnum.SUCCESSFUL;
      case UNSUCCESSFUL -> InterviewResultEnum.UNSUCCESSFUL;
      case ERROR -> InterviewResultEnum.ERROR;
    };
  }

  /** Преобразует результат собеседования из DTO */
  @Named("resultToEntity")
  default Interview.Result resultToEntity(InterviewResultEnum result) {
    if (result == null) return null;
    return switch (result) {
      case SUCCESSFUL -> Interview.Result.SUCCESSFUL;
      case UNSUCCESSFUL -> Interview.Result.UNSUCCESSFUL;
      case ERROR -> Interview.Result.ERROR;
    };
  }

  /** Преобразует LocalDateTime в OffsetDateTime для Interview */
  @Named("interviewLocalDateTimeToOffsetDateTime")
  default OffsetDateTime interviewLocalDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
    if (localDateTime == null) return null;
    return localDateTime.atOffset(ZoneOffset.UTC);
  }

  /** Преобразует OffsetDateTime в LocalDateTime для Interview */
  @Named("interviewOffsetDateTimeToLocalDateTime")
  default LocalDateTime interviewOffsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) return null;
    return offsetDateTime.toLocalDateTime();
  }
}
