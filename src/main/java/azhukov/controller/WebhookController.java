package azhukov.controller;

import azhukov.api.WebhooksApi;
import azhukov.model.ElevenLabsWebhookEvent;
import azhukov.service.VoiceInterviewService;
import azhukov.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для обработки webhook событий от внешних сервисов Основной фокус - события от
 * ElevenLabs Conversational AI
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController implements WebhooksApi {

  private final WebhookService webhookService;
  private final VoiceInterviewService voiceInterviewService;

  @Override
  public ResponseEntity<Void> handleElevenLabsWebhook(ElevenLabsWebhookEvent event) {
    // Получаем подпись и IP из запроса
    String signature = null;
    String clientIp = "unknown";

    try {
      if (getRequest().isPresent()) {
        var request = getRequest().get();
        signature = request.getHeader("X-ElevenLabs-Signature");
        clientIp = getClientIpAddress(request.getNativeRequest(HttpServletRequest.class));
      }
    } catch (Exception e) {
      log.debug("Could not get request details: {}", e.getMessage());
    }

    log.info(
        "Received ElevenLabs webhook from {}: type={}, interviewId={}",
        clientIp,
        event.getType(),
        event.getInterviewId());

    try {
      // Валидация подписи webhook (если настроена)
      if (signature != null) {
        try {
          if (getRequest().isPresent()) {
            var httpRequest = getRequest().get().getNativeRequest(HttpServletRequest.class);
            if (!webhookService.validateElevenLabsSignature(event, signature, httpRequest)) {
              log.warn("Invalid webhook signature from {}", clientIp);
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
          }
        } catch (Exception e) {
          log.warn("Error validating signature: {}", e.getMessage());
        }
      }

      // Обработка события в зависимости от типа
      switch (event.getType()) {
        case AGENT_MESSAGE:
          webhookService.handleAgentMessage(event);
          break;
        case AGENT_TOOL_CALL:
          webhookService.handleAgentToolCall(event);
          break;
        case CONVERSATION_STARTED:
          webhookService.handleConversationStarted(event);
          break;
        case CONVERSATION_ENDED:
          webhookService.handleConversationEnded(event);
          break;
        case ERROR:
          webhookService.handleError(event);
          break;
        default:
          log.warn("Unknown webhook event type: {} from {}", event.getType(), clientIp);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }

      log.info(
          "Successfully processed ElevenLabs webhook: type={}, interviewId={}",
          event.getType(),
          event.getInterviewId());
      return ResponseEntity.ok().build();

    } catch (Exception e) {
      log.error(
          "Error processing ElevenLabs webhook from {}: type={}, interviewId={}, error={}",
          clientIp,
          event.getType(),
          event.getInterviewId(),
          e.getMessage(),
          e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Health check endpoint для webhook */
  @GetMapping("/elevenlabs/health")
  public ResponseEntity<Map<String, String>> webhookHealth() {
    return ResponseEntity.ok(
        Map.of(
            "status", "healthy",
            "service", "ElevenLabs Webhook Handler",
            "timestamp", java.time.Instant.now().toString()));
  }

  /** Получение реального IP адреса клиента */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null
        && !xForwardedFor.isEmpty()
        && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}
