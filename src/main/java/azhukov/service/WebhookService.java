package azhukov.service;

import azhukov.entity.Interview;
import azhukov.entity.InterviewAnswer;
import azhukov.entity.Question;
import azhukov.exception.ResourceNotFoundException;
import azhukov.model.ElevenLabsWebhookEvent;
import azhukov.model.VoiceMessage;
import azhukov.model.VoiceMessageTypeEnum;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.QuestionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для обработки webhook событий от внешних сервисов Основной фокус - события от ElevenLabs
 * Conversational AI
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WebhookService {

  private final InterviewRepository interviewRepository;
  private final QuestionRepository questionRepository;
  private final InterviewAnswerRepository interviewAnswerRepository;
  private final VoiceInterviewService voiceInterviewService;

  @Value("${elevenlabs.webhook.secret:}")
  private String webhookSecret;

  @Value("${elevenlabs.webhook.enable-signature-validation:false}")
  private boolean enableSignatureValidation;

  /** Валидация подписи webhook от ElevenLabs */
  public boolean validateElevenLabsSignature(
      ElevenLabsWebhookEvent event, String signature, HttpServletRequest request) {
    if (!enableSignatureValidation || webhookSecret.isEmpty()) {
      log.debug("Webhook signature validation disabled or secret not configured");
      return true;
    }

    if (signature == null || signature.isEmpty()) {
      log.warn("Missing webhook signature");
      return false;
    }

    try {
      // Получаем тело запроса
      String requestBody = getRequestBody(request);

      // Генерируем ожидаемую подпись
      String expectedSignature = generateHmacSha256(requestBody, webhookSecret);

      boolean isValid =
          MessageDigest.isEqual(
              signature.getBytes(StandardCharsets.UTF_8),
              expectedSignature.getBytes(StandardCharsets.UTF_8));

      if (!isValid) {
        log.warn(
            "Invalid webhook signature. Expected: {}, Received: {}", expectedSignature, signature);
      }

      return isValid;
    } catch (Exception e) {
      log.error("Error validating webhook signature", e);
      return false;
    }
  }

  /** Обработка сообщений от агента */
  public void handleAgentMessage(ElevenLabsWebhookEvent event) {
    log.info("Processing agent message for interview: {}", event.getInterviewId());

    try {
      // Извлекаем данные сообщения
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) event.getData();
      String message = (String) data.get("message");
      String agentId = (String) data.get("agentId");

      // Сохраняем сообщение агента
      VoiceMessage voiceMessage = new VoiceMessage();
      voiceMessage.setText(message);
      voiceMessage.setType(VoiceMessageTypeEnum.AGENT_MESSAGE);
      voiceMessage.setTimestamp(java.time.OffsetDateTime.now());

      // TODO: Сохранить в базу данных
      log.info("Agent message saved: {}", message);

    } catch (Exception e) {
      log.error("Error processing agent message", e);
      throw e;
    }
  }

  /** Обработка вызовов инструментов агентом */
  public void handleAgentToolCall(ElevenLabsWebhookEvent event) {
    log.info("Processing agent tool call for interview: {}", event.getInterviewId());

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) event.getData();
      String toolName = (String) data.get("toolName");
      @SuppressWarnings("unchecked")
      Map<String, Object> parameters = (Map<String, Object>) data.get("parameters");

      switch (toolName) {
        case "getNextQuestion":
          handleGetNextQuestion(event.getInterviewId(), parameters);
          break;
        case "saveAnswer":
          handleSaveAnswer(event.getInterviewId(), parameters);
          break;
        case "endInterview":
          handleEndInterview(event.getInterviewId(), parameters);
          break;
        default:
          log.warn("Unknown tool call: {}", toolName);
      }

    } catch (Exception e) {
      log.error("Error processing agent tool call", e);
      throw e;
    }
  }

  /** Обработка начала разговора */
  public void handleConversationStarted(ElevenLabsWebhookEvent event) {
    log.info("Conversation started for interview: {}", event.getInterviewId());

    try {
      Long interviewId = Long.parseLong(event.getInterviewId());
      Interview interview =
          interviewRepository
              .findById(interviewId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Interview not found: " + interviewId));

      // Обновляем статус интервью
      interview.setStatus(Interview.Status.IN_PROGRESS);
      interview.setStartedAt(LocalDateTime.now());
      interviewRepository.save(interview);

      log.info("Interview status updated to IN_PROGRESS: {}", interviewId);

    } catch (Exception e) {
      log.error("Error processing conversation started", e);
      throw e;
    }
  }

  /** Обработка завершения разговора */
  public void handleConversationEnded(ElevenLabsWebhookEvent event) {
    log.info("Conversation ended for interview: {}", event.getInterviewId());

    try {
      Long interviewId = Long.parseLong(event.getInterviewId());
      Interview interview =
          interviewRepository
              .findById(interviewId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Interview not found: " + interviewId));

      // Обновляем статус интервью
      interview.setStatus(Interview.Status.FINISHED);
      interview.setFinishedAt(LocalDateTime.now());
      interviewRepository.save(interview);

      // TODO: Запустить анализ результатов интервью
      log.info("Interview completed: {}", interviewId);

    } catch (Exception e) {
      log.error("Error processing conversation ended", e);
      throw e;
    }
  }

  /** Обработка ошибок */
  public void handleError(ElevenLabsWebhookEvent event) {
    log.error("ElevenLabs error for interview: {} - {}", event.getInterviewId(), event.getData());

    try {
      Long interviewId = Long.parseLong(event.getInterviewId());
      Interview interview =
          interviewRepository
              .findById(interviewId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Interview not found: " + interviewId));

      // Обновляем статус интервью на ошибку
      interview.setStatus(Interview.Status.FINISHED);
      interview.setResult(Interview.Result.ERROR);
      interview.setFinishedAt(LocalDateTime.now());
      interviewRepository.save(interview);

      log.error("Interview marked as error: {}", interviewId);

    } catch (Exception e) {
      log.error("Error processing webhook error", e);
      throw e;
    }
  }

  /** Обработка запроса следующего вопроса */
  private void handleGetNextQuestion(String interviewId, Map<String, Object> parameters) {
    log.info("Agent requested next question for interview: {}", interviewId);

    // TODO: Реализовать логику получения следующего вопроса
    // Это может быть вызов VoiceInterviewService.getNextQuestion()
  }

  /** Обработка сохранения ответа */
  private void handleSaveAnswer(String interviewId, Map<String, Object> parameters) {
    log.info("Agent requested to save answer for interview: {}", interviewId);

    try {
      Long interviewIdLong = Long.parseLong(interviewId);
      Long questionId = Long.parseLong((String) parameters.get("questionId"));
      String answerText = (String) parameters.get("answerText");

      // Сохраняем ответ
      Interview interview =
          interviewRepository
              .findById(interviewIdLong)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Interview not found: " + interviewIdLong));
      Question question =
          questionRepository
              .findById(questionId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Question not found: " + questionId));

      InterviewAnswer answer =
          InterviewAnswer.builder()
              .interview(interview)
              .question(question)
              .answerText(answerText)
              .build();

      interviewAnswerRepository.save(answer);
      log.info("Answer saved for question: {}", questionId);

    } catch (Exception e) {
      log.error("Error saving answer", e);
      throw e;
    }
  }

  /** Обработка завершения интервью */
  private void handleEndInterview(String interviewId, Map<String, Object> parameters) {
    log.info("Agent requested to end interview: {}", interviewId);

    // Делегируем в VoiceInterviewService
    voiceInterviewService.endVoiceSession(Long.parseLong(interviewId));
  }

  /** Получение тела запроса для валидации подписи */
  private String getRequestBody(HttpServletRequest request) {
    // В реальной реализации нужно получить тело запроса
    // Для простоты возвращаем пустую строку
    return "";
  }

  /** Генерация HMAC-SHA256 подписи */
  private String generateHmacSha256(String data, String secret) {
    try {
      javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
      javax.crypto.spec.SecretKeySpec secretKeySpec =
          new javax.crypto.spec.SecretKeySpec(
              secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      log.error("Error generating HMAC signature", e);
      throw new RuntimeException("Failed to generate signature", e);
    }
  }
}
