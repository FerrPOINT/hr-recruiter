package azhukov.controller;

import azhukov.api.AiApi;
import azhukov.mapper.AiOpenApiMapper;
import azhukov.mapper.PositionMapper;
import azhukov.model.GenerateQuestionsRequest;
import azhukov.model.PositionDataGenerationRequest;
import azhukov.model.PositionDataGenerationResponse;
import azhukov.model.Question;
import azhukov.model.TranscribeAnswerWithAI200Response;
import azhukov.model.TranscribeAudio200Response;
import azhukov.service.PositionDataGenerationService;
import azhukov.service.RewriteService;
import azhukov.service.TranscriptionService;
import azhukov.service.WhisperService;
import azhukov.service.ai.AIService;
import azhukov.service.ai.openrouter.OpenRouterService;
import azhukov.util.EnumUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
  private final RewriteService rewriteService;
  private final WhisperService whisperService;
  private final TranscriptionService transcriptionService;
  private final PositionDataGenerationService positionDataGenerationService;
  private final AiOpenApiMapper aiOpenApiMapper;
  private final PositionMapper positionMapper;
  private final OpenRouterService openRouterService;

  @Override
  public ResponseEntity<List<Question>> generateQuestions(GenerateQuestionsRequest request) {
    // В контроллере не должно быть бизнес-логики — только делегирование сервису и маппинг моделей.
    // Вся логика генерации и проверки доступности AI — только в сервисе.
    try {
      // Генерируем вопросы через сервис (бизнес-логика и проверки внутри сервиса)
      List<String> questions = aiService.generateQuestions(request.getPositionDescription(), 10);
      // Маппим строки в openapi-модель Question (минимальный набор полей)
      List<Question> questionModels = new java.util.ArrayList<>();
      long order = 1;
      for (String q : questions) {
        Question question = new Question();
        question.setText(q.trim());
        question.setType(azhukov.model.QuestionTypeEnum.TEXT);
        question.setOrder(order++);
        question.setIsRequired(true);
        questionModels.add(question);
      }
      return ResponseEntity.ok(questionModels);
    } catch (Exception e) {
      // Ошибки AI/доступности — сервис сам выбрасывает исключения
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<PositionDataGenerationResponse> generatePositionData(
      PositionDataGenerationRequest request) {
    log.info("Запрос на генерацию данных вакансии для описания: {}", request.getDescription());
    try {
      azhukov.model.PositionDataGenerationResponse serviceResponse =
          positionDataGenerationService.generatePositionData(request);
      return ResponseEntity.ok(serviceResponse);
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("AI сервис недоступен")) {
        log.warn("AI сервис недоступен");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }
      log.error("Ошибка при генерации данных вакансии", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<TranscribeAudio200Response> transcribeAudio(MultipartFile audio) {
    try {
      String transcription = whisperService.processAudioFile(audio);
      TranscribeAudio200Response response = new TranscribeAudio200Response();
      response.setTranscript(transcription);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error in transcribe endpoint", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<TranscribeAnswerWithAI200Response> transcribeAnswerWithAI(
      MultipartFile audioFile, Long interviewId, Long questionId) {
    try {
      log.info(
          "Processing audio transcription for interview ID: {} question ID: {}, file: {} ({} bytes)",
          interviewId,
          questionId,
          audioFile.getOriginalFilename(),
          audioFile.getSize());

      // Обрабатываем аудио через пайплайн: Whisper -> Claude -> БД
      // Создаем InterviewAnswer и сохраняем результат
      Long interviewAnswerId =
          transcriptionService.processAudioTranscription(audioFile, interviewId, questionId);

      // Получаем отформатированный текст из созданного ответа
      String formattedText = transcriptionService.getFormattedTranscription(interviewAnswerId);

      // Формируем ответ используя сгенерированную модель из OpenAPI
      TranscribeAnswerWithAI200Response response = new TranscribeAnswerWithAI200Response();
      response.setSuccess(true);
      response.setFormattedText(formattedText);
      response.setInterviewAnswerId(interviewAnswerId);

      log.info(
          "Audio transcription completed successfully for interview ID: {}, question ID: {}, created answer ID: {}",
          interviewId,
          questionId,
          interviewAnswerId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request parameters: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        log.warn(
            "Interview or question not found: interviewId={}, questionId={}",
            interviewId,
            questionId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }
      log.error("Error in transcribeAnswerWithAI endpoint", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Диагностический endpoint для тестирования передачи аудио файлов POST
   * /api/v1/ai/test-audio-upload
   */
  @PostMapping("/test-audio-upload")
  public ResponseEntity<Map<String, Object>> testAudioUpload(
      @RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();

    try {
      log.info(
          "Testing audio file upload: {} ({} bytes, {})",
          file.getOriginalFilename(),
          file.getSize(),
          file.getContentType());

      // Проверяем файл
      if (file.isEmpty()) {
        response.put("success", false);
        response.put("error", "File is empty");
        return ResponseEntity.badRequest().body(response);
      }

      // Проверяем тип файла
      String contentType = file.getContentType();
      if (contentType == null || !contentType.startsWith("audio/")) {
        response.put("success", false);
        response.put("error", "Invalid file type: " + contentType);
        return ResponseEntity.badRequest().body(response);
      }

      // Проверяем размер файла
      if (file.getSize() > 50 * 1024 * 1024) {
        response.put("success", false);
        response.put("error", "File too large: " + file.getSize() + " bytes");
        return ResponseEntity.badRequest().body(response);
      }

      response.put("success", true);
      response.put("filename", file.getOriginalFilename());
      response.put("size", file.getSize());
      response.put("contentType", contentType);
      response.put("message", "Audio file received successfully");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error in test audio upload", e);
      response.put("success", false);
      response.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  /**
   * Генерирует вакансию на основе описания пользователя. Если описание содержит мало информации или
   * бессмыслицу, генерирует хотя бы название вакансии.
   */
  @PostMapping("/generate-position")
  public ResponseEntity<azhukov.model.PositionAiGenerationResponse> generatePosition(
      @RequestBody azhukov.model.PositionAiGenerationRequest request) {
    try {
      String description = request.getDescription();
      Long questionsCount = request.getQuestionsCount() != null ? request.getQuestionsCount() : 5L;
      String questionType = request.getQuestionType() != null ? request.getQuestionType() : "hard";

      if (description == null || description.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      log.info(
          "Generating position from description: {}, questionsCount: {}, questionType: {}",
          description.substring(0, Math.min(100, description.length())),
          questionsCount,
          questionType);

      // Генерируем вакансию через AI
      azhukov.service.ai.openrouter.dto.PositionGenerationResponse aiResponse =
          openRouterService.generatePositionWithAI(
              description, questionsCount.intValue(), questionType);

      // Маппим в OpenAPI модель
      azhukov.model.PositionAiGenerationResponse response =
          new azhukov.model.PositionAiGenerationResponse();
      response.setTitle(aiResponse.getTitle());
      response.setDescription(aiResponse.getDescription());
      response.setTopics(aiResponse.getTopics());
      response.setLevel(
          EnumUtils.safeValueOf(
              aiResponse.getLevel(),
              azhukov.model.PositionAiGenerationResponse.LevelEnum.class,
              azhukov.model.PositionAiGenerationResponse.LevelEnum.MIDDLE));

      // Маппим вопросы
      List<azhukov.model.PositionAiQuestion> questions = new ArrayList<>();
      if (aiResponse.getQuestions() != null) {
        for (azhukov.service.ai.openrouter.dto.PositionGenerationResponse.Question aiQuestion :
            aiResponse.getQuestions()) {
          azhukov.model.PositionAiQuestion question = new azhukov.model.PositionAiQuestion();
          question.setText(aiQuestion.getText());
          question.setType(
              EnumUtils.safeValueOf(
                  aiQuestion.getType(),
                  azhukov.model.PositionAiQuestion.TypeEnum.class,
                  azhukov.model.PositionAiQuestion.TypeEnum.TEXT));
          question.setOrder(aiQuestion.getOrder() != null ? aiQuestion.getOrder().longValue() : 1L);
          questions.add(question);
        }
      }
      response.setQuestions(questions);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error generating position", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
