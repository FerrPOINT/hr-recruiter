package azhukov.service;

import azhukov.config.ElevenLabsProperties;
import azhukov.entity.Interview;
import azhukov.entity.InterviewAnswer;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.model.VoiceMessage;
import azhukov.model.VoiceSessionResponse;
import azhukov.model.VoiceSessionStatusEnum;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.QuestionRepository;
import azhukov.service.ai.elevenlabs.ElevenLabsService;
import azhukov.service.ai.elevenlabs.dto.VoiceMessage.MessageType;
import azhukov.service.ai.elevenlabs.dto.VoiceSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/** Сервис для управления голосовыми интервью с ElevenLabs Conversational AI */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VoiceInterviewService {

  private final InterviewRepository interviewRepository;
  private final QuestionRepository questionRepository;
  private final InterviewAnswerRepository interviewAnswerRepository;
  private final ElevenLabsService elevenLabsService;
  private final ElevenLabsProperties properties;

  @Qualifier("elevenLabsRestTemplate")
  private final RestTemplate elevenLabsRestTemplate;

  private final ObjectMapper objectMapper;

  /** Создает голосовую сессию для интервью */
  public VoiceSessionResponse createVoiceSession(
      Long interviewId, azhukov.model.VoiceSessionCreateRequest voiceSessionCreateRequest) {
    log.info("Creating voice session for interview: {}", interviewId);

    // Проверяем конфигурацию ElevenLabs
    if (!properties.isValid()) {
      throw new ValidationException("ElevenLabs configuration is invalid: API key is required");
    }

    if (properties.getDefaultAgentId() == null || properties.getDefaultAgentId().trim().isEmpty()) {
      throw new ValidationException("ElevenLabs agent ID is not configured");
    }

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    // Проверяем, что интервью еще не началось
    if (interview.getStatus() != Interview.Status.NOT_STARTED) {
      throw new ValidationException("Interview already started or finished");
    }

    // Проверяем, что у позиции есть вопросы
    List<Question> questions =
        questionRepository.findByPositionOrderByOrderAsc(interview.getPosition());
    if (questions.isEmpty()) {
      throw new ValidationException("No questions found for position");
    }

    try {
      // Создаем сессию в ElevenLabs
      VoiceSessionRequest request = buildVoiceSessionRequest(interview, questions);
      azhukov.service.ai.elevenlabs.dto.VoiceSessionResponse response =
          createElevenLabsSession(request);

      // Маппинг ElevenLabs DTO -> openapi-модель
      VoiceSessionResponse apiResponse = new VoiceSessionResponse();
      apiResponse.setSessionId(response.getSessionId());
      apiResponse.setStatus(VoiceSessionStatusEnum.fromValue(response.getStatus()));
      apiResponse.setAgentId(response.getAgentId());
      apiResponse.setWebhookUrl("/api/v1/webhooks/elevenlabs/events");
      apiResponse.setCreatedAt(java.time.OffsetDateTime.now());

      // Обновляем интервью
      interview.setVoiceEnabled(true);
      interview.setVoiceSessionId(response.getSessionId());
      interview.setVoiceAgentId(response.getAgentId());
      interview.setVoiceVoiceId(response.getVoiceId());
      interview.setVoiceLanguage(properties.getDefaultLanguage());
      interview.setVoiceStartedAt(LocalDateTime.now());
      interview.setStatus(Interview.Status.IN_PROGRESS);
      interview.start();

      interviewRepository.save(interview);

      log.info(
          "Voice session created successfully: {} for interview: {}",
          response.getSessionId(),
          interviewId);

      return apiResponse;

    } catch (Exception e) {
      log.error("Failed to create voice session for interview: {}", interviewId, e);
      throw new RuntimeException("Failed to create voice session: " + e.getMessage(), e);
    }
  }

  /** Получает следующий вопрос для голосовой сессии */
  public VoiceMessage getNextQuestion(Long interviewId) {
    log.info("Getting next question for voice session: {}", interviewId);

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    if (!interview.getVoiceEnabled()) {
      throw new ValidationException("Voice interview not enabled for this interview");
    }

    // Получаем вопросы для позиции
    List<Question> questions =
        questionRepository.findByPositionOrderByOrderAsc(interview.getPosition());

    // Находим следующий вопрос (который еще не задавался)
    List<InterviewAnswer> existingAnswers =
        interviewAnswerRepository.findByInterviewId(interviewId);
    int nextQuestionIndex = existingAnswers.size();

    if (nextQuestionIndex >= questions.size()) {
      // Все вопросы заданы, завершаем интервью
      log.info("All questions completed for interview: {}, finishing interview", interviewId);
      // Завершаем интервью
      interview.finish(Interview.Result.SUCCESSFUL);
      interviewRepository.save(interview);
      azhukov.service.ai.elevenlabs.dto.VoiceMessage serviceMsg =
          azhukov.service.ai.elevenlabs.dto.VoiceMessage.builder()
              .type(MessageType.SESSION_END)
              .sessionId(interview.getVoiceSessionId())
              .text("Спасибо за участие в собеседовании! Интервью завершено.")
              .timestamp(System.currentTimeMillis())
              .build();
      VoiceMessage apiMsg = new VoiceMessage();
      apiMsg.setText(serviceMsg.getText());
      return apiMsg;
    }
    Question nextQuestion = questions.get(nextQuestionIndex);
    azhukov.service.ai.elevenlabs.dto.VoiceMessage serviceMsg =
        azhukov.service.ai.elevenlabs.dto.VoiceMessage.builder()
            .type(MessageType.AGENT_QUESTION)
            .sessionId(interview.getVoiceSessionId())
            .questionId(nextQuestion.getId())
            .text(nextQuestion.getText())
            .timestamp(System.currentTimeMillis())
            .build();
    VoiceMessage apiMsg = new VoiceMessage();
    apiMsg.setText(serviceMsg.getText());
    return apiMsg;
  }

  /** Сохраняет голосовой ответ кандидата */
  public InterviewAnswer saveVoiceAnswer(
      Long interviewId, Long questionId, VoiceMessage voiceMessage) {
    log.info("Saving voice answer for interview: {}, question: {}", interviewId, questionId);

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    Question question =
        questionRepository
            .findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    if (!interview.getVoiceEnabled()) {
      throw new ValidationException("Voice interview not enabled for this interview");
    }

    // Преобразование openapi VoiceMessage -> ElevenLabs DTO
    azhukov.service.ai.elevenlabs.dto.VoiceMessage serviceMsg =
        azhukov.service.ai.elevenlabs.dto.VoiceMessage.builder()
            .type(MessageType.CANDIDATE_ANSWER)
            .text(voiceMessage.getText())
            // остальные поля по необходимости
            .build();
    // audio не поддерживается напрямую
    return saveVoiceAnswerInternal(interviewId, questionId, serviceMsg);
  }

  // Внутренний метод для работы с ElevenLabs DTO
  private InterviewAnswer saveVoiceAnswerInternal(
      Long interviewId,
      Long questionId,
      azhukov.service.ai.elevenlabs.dto.VoiceMessage voiceMessage) {
    // Создаем ответ
    InterviewAnswer answer =
        InterviewAnswer.builder()
            .interview(interviewRepository.findById(interviewId).orElseThrow())
            .question(questionRepository.findById(questionId).orElseThrow())
            .answerText(voiceMessage.getText())
            .voiceSessionId(voiceMessage.getSessionId())
            .voiceConfidence(voiceMessage.getConfidence())
            .voiceEmotion(voiceMessage.getEmotion())
            .voiceSpeakerId(voiceMessage.getSpeakerId())
            .voiceProcessingTime(voiceMessage.getDurationMs())
            .voiceQualityScore(voiceMessage.getAudioQuality())
            .durationSeconds(
                voiceMessage.getDurationMs() != null
                    ? (int) (voiceMessage.getDurationMs() / 1000)
                    : null)
            .build();

    // Мониторинг: логировать длительные/сомнительные ответы
    Integer durationSec = answer.getDurationSeconds();
    Double confidence = answer.getVoiceConfidence();
    if (durationSec != null && durationSec > properties.getMaxAnswerDurationSeconds()) {
      log.warn(
          "Voice answer duration {}s exceeds max {}s for interview {}, question {}",
          durationSec,
          properties.getMaxAnswerDurationSeconds(),
          interviewId,
          questionId);
    }
    if (confidence != null && confidence < properties.getMinConfidenceThreshold()) {
      log.warn(
          "Voice answer confidence {} below threshold {} for interview {}, question {}",
          confidence,
          properties.getMinConfidenceThreshold(),
          interviewId,
          questionId);
    }

    InterviewAnswer savedAnswer = interviewAnswerRepository.save(answer);
    Interview interviewEntity = interviewRepository.findById(interviewId).orElseThrow();
    interviewEntity.addAnswer(savedAnswer);
    interviewRepository.save(interviewEntity);

    log.info(
        "Voice answer saved successfully: {} for question: {}", savedAnswer.getId(), questionId);

    return savedAnswer;
  }

  /** Получает статус голосовой сессии */
  public azhukov.model.VoiceSessionStatus getVoiceSessionStatus(Long interviewId) {
    log.info("Getting voice session status for interview: {}", interviewId);

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    azhukov.model.VoiceSessionStatus status = new azhukov.model.VoiceSessionStatus();

    if (!interview.getVoiceEnabled()) {
      status.setStatus(VoiceSessionStatusEnum.ERROR);
    } else if (interview.getStatus() == Interview.Status.NOT_STARTED) {
      status.setStatus(VoiceSessionStatusEnum.CREATED);
    } else if (interview.getStatus() == Interview.Status.IN_PROGRESS) {
      status.setStatus(VoiceSessionStatusEnum.ACTIVE);
    } else if (interview.getStatus() == Interview.Status.FINISHED) {
      status.setStatus(VoiceSessionStatusEnum.ENDED);
    } else {
      status.setStatus(VoiceSessionStatusEnum.ERROR);
    }

    return status;
  }

  /** Завершает голосовую сессию */
  public void endVoiceSession(Long interviewId) {
    log.info("Ending voice session for interview: {}", interviewId);

    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Interview not found: " + interviewId));

    if (!interview.getVoiceEnabled()) {
      throw new ValidationException("Voice interview not enabled for this interview");
    }

    // Завершаем сессию в ElevenLabs
    try {
      endElevenLabsSession(interview.getVoiceSessionId());
    } catch (Exception e) {
      log.error(
          "Failed to end ElevenLabs session for interview {}: {}", interviewId, e.getMessage(), e);
    }

    // Обновляем интервью
    interview.setVoiceFinishedAt(LocalDateTime.now());
    if (interview.getVoiceStartedAt() != null) {
      interview.setVoiceTotalDuration(
          java.time.Duration.between(interview.getVoiceStartedAt(), interview.getVoiceFinishedAt())
              .getSeconds());
    }

    interviewRepository.save(interview);

    log.info("Voice session ended successfully for interview: {}", interviewId);
  }

  /** Строит запрос для создания сессии в ElevenLabs */
  private VoiceSessionRequest buildVoiceSessionRequest(
      Interview interview, List<Question> questions) {
    // Создаем инструмент для получения следующего вопроса
    VoiceSessionRequest.Tool nextQuestionTool =
        VoiceSessionRequest.Tool.builder()
            .type("webhook")
            .name("getNextQuestion")
            .description("Получить следующий вопрос для интервью")
            .webhookUrl("/api/interviews/" + interview.getId() + "/voice/next-question")
            .build();

    return VoiceSessionRequest.builder()
        .agentId(properties.getDefaultAgentId())
        .voiceId(properties.getDefaultVoiceId())
        .language(properties.getDefaultLanguage())
        .prompt(properties.getDefaultAgentPrompt())
        .voiceSettings(
            VoiceSessionRequest.VoiceSettings.builder()
                .stability(properties.getStability())
                .similarityBoost(properties.getSimilarityBoost())
                .style(properties.getStyle())
                .useSpeakerBoost(true)
                .build())
        .sessionSettings(
            VoiceSessionRequest.SessionSettings.builder()
                .maxDurationMinutes(properties.getMaxSessionDurationMinutes())
                .responseTimeoutSeconds(properties.getResponseTimeoutSeconds())
                .enableEmotionDetection(properties.isEnableEmotionDetection())
                .enableSpeakerDetection(properties.isEnableSpeakerDetection())
                .enableTimestamps(properties.isEnableTimestamps())
                .enableAudioQualityAnalysis(properties.isEnableAudioQualityAnalysis())
                .build())
        .tools(new VoiceSessionRequest.Tool[] {nextQuestionTool})
        .build();
  }

  /** Создает сессию в ElevenLabs */
  private azhukov.service.ai.elevenlabs.dto.VoiceSessionResponse createElevenLabsSession(
      VoiceSessionRequest request) {
    // Правильный URL для ElevenLabs Conversational AI
    String url = properties.getApiUrl() + "/v1/convai/agents/" + request.getAgentId() + "/sessions";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("xi-api-key", properties.getApiKey());

    HttpEntity<VoiceSessionRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<azhukov.service.ai.elevenlabs.dto.VoiceSessionResponse> response =
          elevenLabsRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              azhukov.service.ai.elevenlabs.dto.VoiceSessionResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody();
      } else {
        log.error("ElevenLabs API returned unexpected status: {}", response.getStatusCode());
        throw new RuntimeException(
            "Failed to create ElevenLabs session: " + response.getStatusCode());
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
      log.error("Unexpected error creating ElevenLabs session", e);
      throw new RuntimeException("Failed to create ElevenLabs session: " + e.getMessage(), e);
    }
  }

  /** Завершает сессию в ElevenLabs */
  private void endElevenLabsSession(String sessionId) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      log.warn("Cannot end ElevenLabs session: sessionId is null or empty");
      return;
    }

    // Правильный URL для завершения сессии
    String url = properties.getApiUrl() + "/v1/sessions/" + sessionId + "/end";

    HttpHeaders headers = new HttpHeaders();
    headers.set("xi-api-key", properties.getApiKey());

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      elevenLabsRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
      log.info("Successfully ended ElevenLabs session: {}", sessionId);
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.warn(
          "ElevenLabs API client error ending session {}: status={}, body={}",
          sessionId,
          e.getStatusCode(),
          e.getResponseBodyAsString());
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.warn(
          "ElevenLabs API server error ending session {}: status={}, body={}",
          sessionId,
          e.getStatusCode(),
          e.getResponseBodyAsString());
    } catch (Exception e) {
      log.warn("Unexpected error ending ElevenLabs session: {}", sessionId, e);
    }
  }
}
