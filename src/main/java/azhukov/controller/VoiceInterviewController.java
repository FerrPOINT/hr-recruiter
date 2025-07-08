package azhukov.controller;

import azhukov.api.VoiceInterviewsApi;
import azhukov.exception.ResourceNotFoundException;
import azhukov.exception.ValidationException;
import azhukov.mapper.InterviewAnswerMapper;
import azhukov.model.InterviewAnswer;
import azhukov.model.VoiceMessage;
import azhukov.model.VoiceSessionResponse;
import azhukov.model.VoiceSessionStatus;
import azhukov.service.VoiceInterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Контроллер для управления голосовыми интервью с ElevenLabs Conversational AI */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
public class VoiceInterviewController extends BaseController implements VoiceInterviewsApi {

  private final VoiceInterviewService voiceInterviewService;
  private final InterviewAnswerMapper interviewAnswerMapper;

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<VoiceSessionResponse> createVoiceSession(Long interviewId) {
    log.info("Creating voice session for interview: {}", interviewId);

    if (interviewId == null || interviewId <= 0) {
      return ResponseEntity.badRequest().build();
    }

    try {
      VoiceSessionResponse response = voiceInterviewService.createVoiceSession(interviewId);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (ResourceNotFoundException e) {
      log.warn("Interview not found: {}", interviewId);
      return ResponseEntity.notFound().build();
    } catch (ValidationException e) {
      log.warn("Validation error for interview {}: {}", interviewId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error creating voice session for interview: {}", interviewId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<VoiceMessage> getNextQuestion(Long interviewId) {
    log.info("Getting next question for voice session: {}", interviewId);

    if (interviewId == null || interviewId <= 0) {
      return ResponseEntity.badRequest().build();
    }

    try {
      VoiceMessage message = voiceInterviewService.getNextQuestion(interviewId);
      return ResponseEntity.ok(message);
    } catch (ResourceNotFoundException e) {
      log.warn("Interview not found: {}", interviewId);
      return ResponseEntity.notFound().build();
    } catch (ValidationException e) {
      log.warn("Validation error for interview {}: {}", interviewId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error getting next question for interview: {}", interviewId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<InterviewAnswer> saveVoiceAnswer(
      Long interviewId, Long questionId, VoiceMessage voiceMessage) {
    log.info("Saving voice answer for interview: {}, question: {}", interviewId, questionId);

    if (interviewId == null || interviewId <= 0 || questionId == null || questionId <= 0) {
      return ResponseEntity.badRequest().build();
    }

    if (voiceMessage == null
        || voiceMessage.getText() == null
        || voiceMessage.getText().trim().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    try {
      azhukov.entity.InterviewAnswer entityAnswer =
          voiceInterviewService.saveVoiceAnswer(interviewId, questionId, voiceMessage);
      InterviewAnswer dto = interviewAnswerMapper.toDto(entityAnswer);
      return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    } catch (ResourceNotFoundException e) {
      log.warn(
          "Interview or question not found: interviewId={}, questionId={}",
          interviewId,
          questionId);
      return ResponseEntity.notFound().build();
    } catch (ValidationException e) {
      log.warn(
          "Validation error: interviewId={}, questionId={}, error={}",
          interviewId,
          questionId,
          e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error(
          "Error saving voice answer for interview: {}, question: {}", interviewId, questionId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<Void> endVoiceSession(Long interviewId) {
    log.info("Ending voice session for interview: {}", interviewId);

    if (interviewId == null || interviewId <= 0) {
      return ResponseEntity.badRequest().build();
    }

    try {
      voiceInterviewService.endVoiceSession(interviewId);
      return ResponseEntity.ok().build();
    } catch (ResourceNotFoundException e) {
      log.warn("Interview not found: {}", interviewId);
      return ResponseEntity.notFound().build();
    } catch (ValidationException e) {
      log.warn("Validation error for interview {}: {}", interviewId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error ending voice session for interview: {}", interviewId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<VoiceSessionStatus> getVoiceSessionStatus(Long interviewId) {
    log.info("Getting voice session status for interview: {}", interviewId);

    if (interviewId == null || interviewId <= 0) {
      return ResponseEntity.badRequest().build();
    }

    try {
      // Получаем статус голосовой сессии из сервиса
      VoiceSessionStatus status = voiceInterviewService.getVoiceSessionStatus(interviewId);
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error getting voice session status for interview: {}", interviewId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
