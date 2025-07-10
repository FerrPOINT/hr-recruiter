package azhukov.mapper;

import azhukov.entity.Agent;
import azhukov.model.AgentConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Маппер для преобразования между Agent entity и Agent model */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentMapper {

  private final ObjectMapper objectMapper;

  /** Преобразует Agent entity в Agent model */
  public azhukov.model.Agent toModel(Agent entity) {
    if (entity == null) {
      return null;
    }

    azhukov.model.Agent model = new azhukov.model.Agent();
    model.setId(entity.getId());
    model.setName(entity.getName());
    model.setDescription(entity.getDescription());
    model.setElevenLabsAgentId(entity.getElevenLabsAgentId());
    model.setStatus(entity.getStatus());
    model.setInterviewId(entity.getInterviewId());
    model.setPositionId(entity.getPositionId());
    if (entity.getCreatedAt() != null) {
      model.setCreatedAt(entity.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toOffsetDateTime());
    }
    if (entity.getUpdatedAt() != null) {
      model.setUpdatedAt(entity.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toOffsetDateTime());
    }

    // Преобразуем config из JSON строки в объект
    if (entity.getConfig() != null) {
      try {
        AgentConfig config = objectMapper.readValue(entity.getConfig(), AgentConfig.class);
        model.setConfig(config);
      } catch (JsonProcessingException e) {
        log.warn("Failed to parse agent config JSON: {}", entity.getConfig(), e);
        // Оставляем config как null если не удалось распарсить
      }
    }

    return model;
  }

  /** Преобразует Agent model в Agent entity */
  public Agent toEntity(azhukov.model.Agent model) {
    if (model == null) {
      return null;
    }

    Agent entity =
        Agent.builder()
            .id(model.getId())
            .name(model.getName())
            .description(model.getDescription())
            .elevenLabsAgentId(model.getElevenLabsAgentId())
            .status(model.getStatus())
            .interviewId(model.getInterviewId())
            .positionId(model.getPositionId())
            .build();

    // Устанавливаем даты отдельно, так как они могут быть OffsetDateTime
    if (model.getCreatedAt() != null) {
      entity.setCreatedAt(model.getCreatedAt().toLocalDateTime());
    }
    if (model.getUpdatedAt() != null) {
      entity.setUpdatedAt(model.getUpdatedAt().toLocalDateTime());
    }

    // Преобразуем config из объекта в JSON строку
    if (model.getConfig() != null) {
      try {
        String configJson = objectMapper.writeValueAsString(model.getConfig());
        entity.setConfig(configJson);
      } catch (JsonProcessingException e) {
        log.warn("Failed to serialize agent config to JSON", e);
        // Оставляем config как null если не удалось сериализовать
      }
    }

    return entity;
  }
}
