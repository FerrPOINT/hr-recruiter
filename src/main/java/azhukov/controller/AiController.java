package azhukov.controller;

import azhukov.api.AiApi;
import azhukov.model.GenerateQuestionsRequest;
import azhukov.model.Question;
import azhukov.model.QuestionTypeEnum;
import azhukov.model.TranscribeAudio200Response;
import azhukov.model.TranscribeInterviewAnswer200Response;
import azhukov.model.TranscribeInterviewAnswer400Response;
import azhukov.model.TranscribeInterviewAnswer503Response;
import azhukov.service.RewriteService;
import azhukov.service.TranscriptionService;
import azhukov.service.WhisperService;
import azhukov.service.ai.AIService;
import azhukov.service.ai.claude.ClaudeService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Контроллер для AI-функциональности Реализует интерфейс AiApi, сгенерированный по OpenAPI
 * спецификации
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AiController implements AiApi {

  private final AIService aiService;
  private final ClaudeService claudeService;
  private final RewriteService rewriteService;
  private final WhisperService whisperService;
  private final TranscriptionService transcriptionService;

  @Override
  public ResponseEntity<List<Question>> generateQuestions(GenerateQuestionsRequest request) {
    log.info("Запрос на генерацию вопросов для описания: {}", request.getPositionDescription());

    try {
      // Проверяем доступность AI сервиса
      if (!aiService.isAvailable()) {
        log.warn("AI сервис недоступен");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      // Используем Claude для генерации вопросов
      String prompt =
          String.format(
              "Сгенерируй %d профессиональных вопросов для собеседования на основе следующего описания вакансии:\n\n%s\n\n"
                  + "Вопросы должны быть разнообразными и покрывать технические навыки, soft skills и опыт работы. "
                  + "Верни только список вопросов, каждый с новой строки.",
              10, // По умолчанию 10 вопросов
              request.getPositionDescription());

      String response = claudeService.generateText(prompt);
      List<String> questions = List.of(response.split("\n"));

      // Преобразуем в модель Question
      List<Question> questionModels =
          questions.stream()
              .map(
                  q -> {
                    Question question = new Question();
                    question.setId(UUID.randomUUID());
                    question.setText(q.trim());
                    question.setType(QuestionTypeEnum.TEXT);
                    question.setOrder(questions.indexOf(q) + 1);
                    question.setIsRequired(true);
                    return question;
                  })
              .toList();

      log.info("Сгенерировано {} вопросов", questionModels.size());
      return ResponseEntity.ok(questionModels);

    } catch (Exception e) {
      log.error("Ошибка при генерации вопросов", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<TranscribeAudio200Response> transcribeAudio(MultipartFile audio) {
    log.info(
        "Запрос на транскрипцию аудио файла: {}",
        audio != null ? audio.getOriginalFilename() : "null");

    try {
      // Проверяем доступность AI сервиса
      if (!aiService.isAvailable()) {
        log.warn("AI сервис недоступен");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      // Проверяем файл
      if (audio == null || audio.isEmpty()) {
        log.warn("Аудио файл не предоставлен или пуст");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }

      // Используем WhisperService для транскрипции
      String transcript = whisperService.transcribeAudio(audio);

      TranscribeAudio200Response response = new TranscribeAudio200Response();
      response.setTranscript(transcript);

      log.info("Аудио успешно транскрибировано, длина текста: {} символов", transcript.length());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Ошибка при транскрипции аудио", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<TranscribeInterviewAnswer200Response> transcribeInterviewAnswer(
      MultipartFile audioFile, UUID interviewAnswerId) {
    
    try {
      log.info("Received transcription request for answer ID: {}", interviewAnswerId);
      
      // Валидация входных данных
      if (audioFile.isEmpty()) {
        throw new IllegalArgumentException("Audio file is empty");
      }
      
      if (interviewAnswerId == null) {
        throw new IllegalArgumentException("Interview answer ID is required");
      }

      // Проверка доступности сервисов
      if (!transcriptionService.isPipelineAvailable()) {
        throw new RuntimeException("Transcription services are not available");
      }

      // Обработка транскрибации
      String formattedText = transcriptionService.processAudioTranscription(audioFile, interviewAnswerId.toString());
      
      TranscribeInterviewAnswer200Response response = new TranscribeInterviewAnswer200Response();
      response.setSuccess(true);
      response.setFormattedText(formattedText);
      response.setInterviewAnswerId(interviewAnswerId);
      
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.error("Validation error for answer ID: {}", interviewAnswerId, e);
      throw e; // Spring автоматически вернет 400 Bad Request
    } catch (RuntimeException e) {
      log.error("Service unavailable for answer ID: {}", interviewAnswerId, e);
      throw e; // Spring автоматически вернет 503 Service Unavailable
    } catch (Exception e) {
      log.error("Error processing transcription for answer ID: {}", interviewAnswerId, e);
      throw new RuntimeException("Transcription processing failed: " + e.getMessage());
    }
  }
}
