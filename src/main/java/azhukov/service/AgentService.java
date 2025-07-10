package azhukov.service;

import azhukov.entity.Agent;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.model.*;
import azhukov.repository.AgentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для управления AI-агентами */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AgentService {

  private final AgentRepository agentRepository;
  private final ObjectMapper objectMapper;

  /** Создает нового агента */
  public Agent createAgent(AgentCreateRequest request) {
    log.info("Creating agent: {}", request.getName());

    // Валидация
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new ValidationException("Agent name is required");
    }

    if (request.getConfig() == null) {
      throw new ValidationException("Agent configuration is required");
    }

    // Создаем агента
    Agent agent = new Agent();
    agent.setName(request.getName());
    agent.setDescription(request.getDescription());
    agent.setInterviewId(request.getInterviewId());
    try {
      agent.setConfig(objectMapper.writeValueAsString(request.getConfig()));
    } catch (Exception e) {
      throw new ValidationException("Invalid agent configuration: " + e.getMessage());
    }
    agent.setStatus(AgentStatusEnum.CREATING);
    agent.setCreatedAt(LocalDateTime.now());
    agent.setUpdatedAt(LocalDateTime.now());

    // TODO: Создать агента в ElevenLabs
    // agent.setElevenLabsAgentId(elevenLabsAgentId);

    Agent savedAgent = agentRepository.save(agent);
    log.info("Agent created successfully: {}", savedAgent.getId());

    return savedAgent;
  }

  /** Получает агента по ID */
  public Agent getAgent(Long id) {
    log.info("Getting agent: {}", id);
    return agentRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + id));
  }

  /** Получает пагинированный список агентов */
  public Page<Agent> getAgentsPage(AgentStatusEnum status, Pageable pageable) {
    log.info("Getting agents page with status: {}", status);

    if (status != null) {
      return agentRepository.findByStatus(status, pageable);
    } else {
      return agentRepository.findAll(pageable);
    }
  }

  /** Обновляет агента */
  public Agent updateAgent(Long id, AgentUpdateRequest request) {
    log.info("Updating agent: {}", id);

    Agent agent = getAgent(id);

    if (request.getName() != null) {
      agent.setName(request.getName());
    }

    if (request.getDescription() != null) {
      agent.setDescription(request.getDescription());
    }

    if (request.getConfig() != null) {
      try {
        agent.setConfig(objectMapper.writeValueAsString(request.getConfig()));
      } catch (Exception e) {
        throw new ValidationException("Invalid agent configuration: " + e.getMessage());
      }
    }

    if (request.getStatus() != null) {
      agent.setStatus(request.getStatus());
    }

    agent.setUpdatedAt(LocalDateTime.now());

    Agent updatedAgent = agentRepository.save(agent);
    log.info("Agent updated successfully: {}", updatedAgent.getId());

    return updatedAgent;
  }

  /** Удаляет агента */
  public void deleteAgent(Long id) {
    log.info("Deleting agent: {}", id);

    Agent agent = getAgent(id);

    // TODO: Удалить агента из ElevenLabs
    // elevenLabsService.deleteAgent(agent.getElevenLabsAgentId());

    agent.setStatus(AgentStatusEnum.DELETED);
    agent.setUpdatedAt(LocalDateTime.now());
    agentRepository.save(agent);

    log.info("Agent deleted successfully: {}", id);
  }

  /** Тестирует агента */
  public AgentTestResponse testAgent(Long id, AgentTestRequest request) {
    log.info("Testing agent: {} with message: {}", id, request.getMessage());

    Agent agent = getAgent(id);

    AgentTestResponse response = new AgentTestResponse();
    response.setSuccess(true);
    response.setResponse("Test response from agent: " + agent.getName());
    response.setDuration(1000L); // TODO: Реальное время ответа

    log.info("Agent test completed successfully: {}", id);

    return response;
  }
}
