package azhukov.service;

import azhukov.config.ElevenLabsProperties;
import azhukov.entity.Agent;
import azhukov.entity.Interview;
import azhukov.entity.Position;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.model.AgentConfig;
import azhukov.repository.AgentRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.PositionRepository;
import azhukov.service.ai.elevenlabs.dto.CreateAgentRequest;
import azhukov.service.ai.elevenlabs.dto.CreateAgentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Сервис для автоматического создания и управления агентами ElevenLabs Создает агентов на основе
 * позиций и интервью
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ElevenLabsAgentService {

  private final AgentRepository agentRepository;
  private final InterviewRepository interviewRepository;
  private final PositionRepository positionRepository;
  private final ElevenLabsProperties properties;
  private final ObjectMapper objectMapper;

  @Qualifier("elevenLabsRestTemplate")
  private final RestTemplate elevenLabsRestTemplate;

  /** Создает агента для интервью на основе позиции */
  public Agent createAgentForInterview(Long interviewId) {
    log.info("Creating ElevenLabs agent for interview: {}", interviewId);

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    Position position =
        positionRepository
            .findById(interview.getPosition().getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Position not found: " + interview.getPosition().getId()));

    // Проверяем, не создан ли уже агент для этого интервью
    if (agentRepository.existsByInterviewId(interviewId)) {
      log.warn("Agent already exists for interview: {}", interviewId);
      return agentRepository
          .findByInterviewId(interviewId)
          .orElseThrow(
              () -> new ResourceNotFoundException("Agent not found for interview: " + interviewId));
    }

    try {
      // Создаем конфигурацию агента
      AgentConfig config = buildAgentConfig(position, interview);

      // Создаем агента в ElevenLabs
      String elevenLabsAgentId = createElevenLabsAgent(config);

      // Сохраняем в базу данных
      Agent agent =
          Agent.builder()
              .interviewId(interviewId)
              .positionId(position.getId())
              .elevenLabsAgentId(elevenLabsAgentId)
              .name("Interview Agent - " + position.getTitle())
              .description("AI interviewer for " + position.getTitle() + " position")
              .status(azhukov.model.AgentStatusEnum.ACTIVE)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();

      // Сериализуем конфигурацию в JSON
      try {
        agent.setConfig(objectMapper.writeValueAsString(config));
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize agent config", e);
        throw new RuntimeException("Failed to serialize agent config", e);
      }

      Agent savedAgent = agentRepository.save(agent);

      log.info(
          "Successfully created ElevenLabs agent: {} for interview: {}",
          elevenLabsAgentId,
          interviewId);

      return savedAgent;

    } catch (Exception e) {
      log.error("Failed to create ElevenLabs agent for interview: {}", interviewId, e);
      throw new RuntimeException("Failed to create agent: " + e.getMessage(), e);
    }
  }

  /** Строит конфигурацию агента на основе позиции */
  private AgentConfig buildAgentConfig(Position position, Interview interview) {
    String prompt = buildInterviewPrompt(position);
    String voiceId = selectVoiceForPosition(position);
    String webhookUrl = buildWebhookUrl(interview.getId());

    AgentConfig config = new AgentConfig();
    config.setName("Interview Agent - " + position.getTitle());
    config.setDescription("AI interviewer for " + position.getTitle() + " position");
    config.setPrompt(prompt);
    config.setVoiceId(voiceId);
    config.setLanguage(properties.getLanguage().getCode());
    config.setPersonality("professional");
    config.setTools(Arrays.asList("getNextQuestion", "saveAnswer", "endInterview"));
    config.setWebhookUrl(webhookUrl);

    azhukov.model.VoiceSettings voiceSettings = new azhukov.model.VoiceSettings();
    voiceSettings.setStability((float) properties.getStability());
    voiceSettings.setSimilarityBoost((float) properties.getSimilarityBoost());
    voiceSettings.setStyle((float) properties.getStyle());
    voiceSettings.setUseSpeakerBoost(true);
    config.setVoiceSettings(voiceSettings);

    return config;
  }

  /** Создает промпт для агента на основе позиции */
  private String buildInterviewPrompt(Position position) {
    return String.format(
        """
        Ты проводишь профессиональное собеседование на позицию "%s".

        Инструкции:
        1. Используй инструмент getNextQuestion для получения вопросов
        2. Озвучивай вопросы четко и профессионально
        3. Дождись полного ответа кандидата
        4. Используй saveAnswer для сохранения ответа
        5. Заверши интервью после последнего вопроса

        Позиция: %s
        Уровень: %s
        Язык: %s
        Описание: %s

        Будь вежливым, профессиональным и внимательным интервьюером.
        Не придумывай свои вопросы - используй только те, что получаешь от системы.
        """,
        position.getTitle(),
        position.getTitle(),
        position.getLevel(),
        position.getLanguage(),
        position.getDescription() != null ? position.getDescription() : "Не указано");
  }

  /** Выбирает голос для позиции на основе языка и уровня */
  private String selectVoiceForPosition(Position position) {
    // Логика выбора голоса на основе языка и уровня позиции
    if ("Русский".equals(position.getLanguage())) {
      return switch (position.getLevel()) {
        case SENIOR, LEAD -> "21m00Tcm4TlvDq8ikWAM"; // Rachel для senior/lead
        case JUNIOR, MIDDLE -> "pNInz6obpgDQGcFmaJgB"; // Adam для junior/middle
      };
    } else {
      return "EXAVITQu4vr4xnSDxMaL"; // Bella для английского
    }
  }

  /** Создает URL для webhook событий */
  private String buildWebhookUrl(Long interviewId) {
    // В продакшене должен быть полный URL
    return "/api/v1/webhooks/elevenlabs/events";
  }

  /** Создает агента в ElevenLabs Conversational AI API */
  private String createElevenLabsAgent(AgentConfig config) {
    try {
      // Создаем conversation_config согласно документации
      CreateAgentRequest.ConversationConfig conversationConfig =
          CreateAgentRequest.ConversationConfig.builder()
              .prompt(config.getPrompt())
              .voiceId(config.getVoiceId())
              .language(config.getLanguage())
              .personality(config.getPersonality())
              .tools(config.getTools())
              .webhookUrl(config.getWebhookUrl())
              .voiceSettings(
                  CreateAgentRequest.VoiceSettings.builder()
                      .stability(config.getVoiceSettings().getStability().doubleValue())
                      .similarityBoost(config.getVoiceSettings().getSimilarityBoost().doubleValue())
                      .style(config.getVoiceSettings().getStyle().doubleValue())
                      .useSpeakerBoost(config.getVoiceSettings().getUseSpeakerBoost())
                      .build())
              .build();

      // Создаем platform_settings
      CreateAgentRequest.PlatformSettings platformSettings =
          CreateAgentRequest.PlatformSettings.builder()
              .maxDurationMinutes(60)
              .responseTimeoutSeconds(30)
              .enableEmotionDetection(true)
              .enableSpeakerDetection(false)
              .enableTimestamps(true)
              .enableAudioQualityAnalysis(true)
              .build();

      CreateAgentRequest request =
          CreateAgentRequest.builder()
              .conversationConfig(conversationConfig)
              .platformSettings(platformSettings)
              .name(config.getName())
              .tags(Arrays.asList("interview", "hr", "recruitment"))
              .build();

      String url = properties.getApiUrl() + "/v1/convai/agents/create";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("xi-api-key", properties.getApiKey());

      HttpEntity<CreateAgentRequest> entity = new HttpEntity<>(request, headers);

      ResponseEntity<CreateAgentResponse> response =
          elevenLabsRestTemplate.exchange(url, HttpMethod.POST, entity, CreateAgentResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.info("Created ElevenLabs agent: {}", response.getBody().getAgentId());
        return response.getBody().getAgentId();
      } else {
        log.error("ElevenLabs API returned unexpected status: {}", response.getStatusCode());
        throw new RuntimeException("Failed to create agent: " + response.getStatusCode());
      }

    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error(
          "ElevenLabs API client error: status={}, body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new ValidationException("ElevenLabs API error: " + e.getMessage());
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.error(
          "ElevenLabs API server error: status={}, body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new RuntimeException("ElevenLabs API server error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error creating ElevenLabs agent", e);
      throw new RuntimeException("Failed to create agent: " + e.getMessage(), e);
    }
  }

  /** Удаляет агента из ElevenLabs и базы данных */
  public void deleteAgent(Long agentId) {
    log.info("Deleting ElevenLabs agent: {}", agentId);

    Agent agent =
        agentRepository
            .findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));

    try {
      // Удаляем из ElevenLabs
      deleteElevenLabsAgent(agent.getElevenLabsAgentId());

      // Удаляем из базы данных
      agentRepository.delete(agent);

      log.info("Successfully deleted agent: {}", agentId);

    } catch (Exception e) {
      log.error("Failed to delete agent: {}", agentId, e);
      throw new RuntimeException("Failed to delete agent: " + e.getMessage(), e);
    }
  }

  /** Удаляет агента из ElevenLabs API */
  private void deleteElevenLabsAgent(String elevenLabsAgentId) {
    if (elevenLabsAgentId == null || elevenLabsAgentId.trim().isEmpty()) {
      log.warn("Cannot delete ElevenLabs agent: agentId is null or empty");
      return;
    }

    String url = properties.getApiUrl() + "/v1/agents/" + elevenLabsAgentId;

    HttpHeaders headers = new HttpHeaders();
    headers.set("xi-api-key", properties.getApiKey());

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      elevenLabsRestTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
      log.info("Successfully deleted ElevenLabs agent: {}", elevenLabsAgentId);
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.warn(
          "ElevenLabs API client error deleting agent {}: status={}, body={}",
          elevenLabsAgentId,
          e.getStatusCode(),
          e.getResponseBodyAsString());
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.warn(
          "ElevenLabs API server error deleting agent {}: status={}, body={}",
          elevenLabsAgentId,
          e.getStatusCode(),
          e.getResponseBodyAsString());
    } catch (Exception e) {
      log.warn("Unexpected error deleting ElevenLabs agent: {}", elevenLabsAgentId, e);
    }
  }

  /** Получает агента по ID интервью */
  public Agent getAgentByInterviewId(Long interviewId) {
    return agentRepository
        .findByInterviewId(interviewId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Agent not found for interview: " + interviewId));
  }

  /** Проверяет, существует ли агент для интервью */
  public boolean agentExistsForInterview(Long interviewId) {
    return agentRepository.existsByInterviewId(interviewId);
  }

  /** Обновляет статус агента */
  public void updateAgentStatus(Long agentId, azhukov.model.AgentStatusEnum status) {
    Agent agent =
        agentRepository
            .findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));

    agent.setStatus(status);
    agent.setUpdatedAt(LocalDateTime.now());
    agentRepository.save(agent);

    log.info("Updated agent {} status to: {}", agentId, status);
  }
}
