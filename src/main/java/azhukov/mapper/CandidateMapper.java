package azhukov.mapper;

import azhukov.entity.Candidate;
import azhukov.model.CandidateCreateRequest;
import azhukov.model.CandidateUpdateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.*;

/**
 * Маппер для преобразования между Candidate entity и DTO. Использует MapStruct для автоматической
 * генерации кода маппинга.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CandidateMapper extends CommonMapper {

  /** Преобразует Candidate entity в Candidate DTO. */
  @Mapping(target = "id", source = "id")
  @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(entity.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(entity.getUpdatedAt()))")
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(
      target = "name",
      expression = "java(entity.getFirstName() + \" \" + entity.getLastName())")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "source", source = "source")
  @Mapping(target = "interview", ignore = true) // Поле отсутствует в entity
  @Mapping(target = "positionId", source = "position.id")
  azhukov.model.Candidate toDto(Candidate entity);

  /** Преобразует CandidateCreateRequest в Candidate entity. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "status", ignore = true) // Устанавливается по умолчанию
  @Mapping(target = "resumeUrl", ignore = true)
  @Mapping(target = "coverLetter", ignore = true)
  @Mapping(target = "experienceYears", ignore = true)
  @Mapping(target = "skills", ignore = true)
  @Mapping(target = "position", ignore = true) // Устанавливается в сервисе
  @Mapping(target = "interviews", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Candidate toEntity(CandidateCreateRequest request);

  /** Преобразует CandidateUpdateRequest в Candidate entity. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "resumeUrl", ignore = true)
  @Mapping(target = "coverLetter", ignore = true)
  @Mapping(target = "experienceYears", ignore = true)
  @Mapping(target = "skills", ignore = true)
  @Mapping(target = "position", ignore = true)
  @Mapping(target = "interviews", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Candidate toEntity(CandidateUpdateRequest request);

  /** Обновляет Candidate entity из CandidateUpdateRequest. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "resumeUrl", ignore = true)
  @Mapping(target = "coverLetter", ignore = true)
  @Mapping(target = "experienceYears", ignore = true)
  @Mapping(target = "skills", ignore = true)
  @Mapping(target = "position", ignore = true)
  @Mapping(target = "interviews", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(CandidateUpdateRequest request, @MappingTarget Candidate entity);

  /** Преобразует список Candidate в список CandidateDto */
  List<azhukov.model.Candidate> toDtoList(List<Candidate> entities);

  // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

  /** Преобразует LocalDateTime в OffsetDateTime. */
  default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
    return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
  }
}
