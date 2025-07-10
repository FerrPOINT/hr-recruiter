package azhukov.controller;

import azhukov.api.AgentsApi;
import azhukov.entity.Agent;
import azhukov.mapper.AgentMapper;
import azhukov.model.*;
import azhukov.service.AgentService;
import azhukov.util.PaginationUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления AI-агентами. Реализует интерфейс AgentsApi, сгенерированный из OpenAPI
 * спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentsApiController extends BaseController implements AgentsApi {

  private final AgentService agentService;
  private final AgentMapper agentMapper;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Agent> createAgent(AgentCreateRequest agentCreateRequest) {
    log.info("Creating agent: {}", agentCreateRequest.getName());
    Agent agent = agentService.createAgent(agentCreateRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(agentMapper.toModel(agent));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAgent(Long id) {
    log.info("Deleting agent: {}", id);
    agentService.deleteAgent(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Agent> getAgent(Long id) {
    log.info("Getting agent: {}", id);
    Agent agent = agentService.getAgent(id);
    return ResponseEntity.ok(agentMapper.toModel(agent));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PaginatedResponse> listAgents(
      Optional<Long> page, Optional<Long> size, Optional<AgentStatusEnum> status) {
    log.info(
        "Listing agents with page: {}, size: {}, status: {}",
        page.orElse(0L),
        size.orElse(20L),
        status.orElse(null));

    Pageable pageable = PaginationUtils.createPageableFromOptional(page, size);
    Page<Agent> agents = agentService.getAgentsPage(status.orElse(null), pageable);

    PaginatedResponse response = new PaginatedResponse();
    PaginationUtils.fillPaginationFields(agents, response);

    // Преобразуем entities в models
    response.setContent(
        agents.getContent().stream()
            .map(agentMapper::toModel)
            .collect(java.util.stream.Collectors.toList()));

    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AgentTestResponse> testAgent(Long id, AgentTestRequest agentTestRequest) {
    log.info("Testing agent: {} with message: {}", id, agentTestRequest.getMessage());
    AgentTestResponse response = agentService.testAgent(id, agentTestRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Agent> updateAgent(
      Long id, AgentUpdateRequest agentUpdateRequest) {
    log.info("Updating agent: {}", id);
    Agent agent = agentService.updateAgent(id, agentUpdateRequest);
    return ResponseEntity.ok(agentMapper.toModel(agent));
  }
}
