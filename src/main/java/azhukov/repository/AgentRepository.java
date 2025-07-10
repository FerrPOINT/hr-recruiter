package azhukov.repository;

import azhukov.entity.Agent;
import azhukov.model.AgentStatusEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Репозиторий для работы с AI-агентами */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

  /** Найти агентов по статусу с пагинацией */
  Page<Agent> findByStatus(AgentStatusEnum status, Pageable pageable);

  /** Найти агента по ID интервью */
  Optional<Agent> findByInterviewId(Long interviewId);

  /** Проверить существование агента по ID интервью */
  boolean existsByInterviewId(Long interviewId);

  /** Найти агентов по ID позиции */
  List<Agent> findByPositionId(Long positionId);

  /** Найти агента по ID в ElevenLabs */
  Optional<Agent> findByElevenLabsAgentId(String elevenLabsAgentId);

  /** Найти активных агентов */
  @Query("SELECT a FROM Agent a WHERE a.status = :status")
  List<Agent> findActiveAgents(@Param("status") AgentStatusEnum status);

  /** Найти агентов по названию (поиск) */
  @Query("SELECT a FROM Agent a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  Page<Agent> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}
