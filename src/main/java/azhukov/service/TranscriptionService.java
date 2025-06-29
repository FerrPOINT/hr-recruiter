package azhukov.service;

import azhukov.entity.Interview;
import azhukov.entity.InterviewAnswer;
import azhukov.entity.Position;
import azhukov.entity.Question;
import azhukov.repository.CandidateRepository;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.repository.InterviewRepository;
import azhukov.repository.QuestionRepository;
import azhukov.service.ai.AIService;
import azhukov.service.ai.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Сервис для обработки транскрибации аудио с пайплайном: Whisper (транскрибация) → Claude
 * (форматирование) → БД (сохранение результата)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {

  private final WhisperService whisperService;
  private final AIService aiService;
  private final InterviewRepository interviewRepository;
  private final QuestionRepository questionRepository;
  private final InterviewAnswerRepository interviewAnswerRepository;
  private final CandidateRepository candidateRepository;

  private static final String FORMATTING_PROMPT =
      """
        Ты - ассистент для форматирования транскрибированного текста. Твоя задача - привести текст в читаемый вид, но НЕ добавлять никакой новой информации.

        ПРАВИЛА:
        - Исправляй только очевидные ошибки транскрибации (пунктуация, заглавные буквы)
        - НЕ добавляй слова, которых нет в оригинале
        - НЕ изменяй смысл сказанного
        - Сохраняй разговорный стиль
        - Убирай только явные артефакты (повторы, междометия типа "э-э", "м-м")

        Пример:
        Вход: "э-э ну я работал в компании э-э как её там да в общем занимался разработкой"
        Выход: "Ну я работал в компании, в общем занимался разработкой"

        Вход: "давайте поговорим о вашем опыте работы"
        Выход: "Давайте поговорим о вашем опыте работы"

        Отформатируй следующий текст:
        """;

  /**
   * Пайплайн транскрибации аудио: 1. Whisper — транскрибация аудио в сырой текст (rawTranscription)
   * 2. Claude — форматирование текста для отображения пользователю (formattedTranscription) 3.
   * Создание нового InterviewAnswer и сохранение обоих вариантов в БД
   *
   * <p>ВАЖНО: Сырой текст (rawTranscription) сохраняется только для последующего анализа/отчетов,
   * на фронт не возвращается. Фронту возвращается только форматированный текст (formattedText).
   */
  @Transactional
  public Long processAudioTranscription(
      MultipartFile audioFile, Long interviewId, Long questionId) {

    long startTime = System.currentTimeMillis();

    try {
      log.info(
          "Starting audio transcription pipeline for interview ID: {} question ID: {}, file: {} ({} bytes)",
          interviewId,
          questionId,
          audioFile.getOriginalFilename(),
          audioFile.getSize());

      // Валидация файла
      validateAudioFile(audioFile);
      log.info("Audio file validation passed");

      // Шаг 1: Транскрибация через Whisper
      long whisperStart = System.currentTimeMillis();
      String rawTranscription = whisperService.transcribeAudio(audioFile);
      long whisperTime = System.currentTimeMillis() - whisperStart;

      log.info(
          "Whisper transcription completed in {} ms, length: {} chars",
          whisperTime,
          rawTranscription.length());

      // Шаг 2: Форматирование через Claude
      long claudeStart = System.currentTimeMillis();
      String formattedText = formatTranscription(rawTranscription);
      long claudeTime = System.currentTimeMillis() - claudeStart;

      log.info(
          "Claude formatting completed in {} ms, length: {} chars",
          claudeTime,
          formattedText.length());

      // Шаг 3: Создание InterviewAnswer и сохранение в БД
      long dbStart = System.currentTimeMillis();
      Long interviewAnswerId =
          createAndSaveInterviewAnswer(interviewId, questionId, rawTranscription, formattedText);
      long dbTime = System.currentTimeMillis() - dbStart;

      log.info(
          "InterviewAnswer created and saved to database in {} ms, ID: {}",
          dbTime,
          interviewAnswerId);

      long totalTime = System.currentTimeMillis() - startTime;

      log.info(
          "Total transcription pipeline completed successfully in {} ms (Whisper: {} ms, Claude: {} ms, DB: {} ms)",
          totalTime,
          whisperTime,
          claudeTime,
          dbTime);

      return interviewAnswerId;

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;

      log.error(
          "Error in transcription pipeline for interview ID: {} question ID: {} (took {} ms): {}",
          interviewId,
          questionId,
          totalTime,
          e.getMessage(),
          e);

      throw new RuntimeException("Transcription processing failed", e);
    }
  }

  /**
   * Получает отформатированный текст из InterviewAnswer
   *
   * @param interviewAnswerId ID ответа на интервью
   * @return отформатированный текст
   */
  public String getFormattedTranscription(Long interviewAnswerId) {
    try {
      InterviewAnswer answer =
          interviewAnswerRepository
              .findById(interviewAnswerId)
              .orElseThrow(
                  () -> new RuntimeException("Interview answer not found: " + interviewAnswerId));

      return answer.getFormattedTranscription();
    } catch (Exception e) {
      log.error("Failed to get formatted transcription for answer ID: {}", interviewAnswerId, e);
      throw new RuntimeException("Failed to get formatted transcription", e);
    }
  }

  /** Валидирует аудио файл */
  private void validateAudioFile(MultipartFile audioFile) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is empty or null");
    }

    if (audioFile.getSize() > 50 * 1024 * 1024) { // 50MB limit
      throw new IllegalArgumentException("Audio file too large: " + audioFile.getSize() + " bytes");
    }

    String contentType = audioFile.getContentType();
    if (contentType == null || !contentType.startsWith("audio/")) {
      throw new IllegalArgumentException("Invalid audio file type: " + contentType);
    }

    log.info("Audio file validation passed: {} ({})", audioFile.getOriginalFilename(), contentType);
  }

  /**
   * Форматирует транскрибированный текст через Claude
   *
   * @param rawTranscription сырой транскрибированный текст
   * @return отформатированный текст
   */
  private String formatTranscription(String rawTranscription) {
    try {
      String prompt = FORMATTING_PROMPT + "\n\n" + rawTranscription;
      return aiService.generateText(prompt);
    } catch (AIServiceException e) {
      log.error("Failed to format transcription with AI", e);
      return rawTranscription;
    }
  }

  /**
   * Проверяет доступность всех сервисов в пайплайне
   *
   * @return true если все сервисы доступны
   */
  public boolean isPipelineAvailable() {
    boolean whisperAvailable = whisperService.isServiceAvailable();
    boolean claudeAvailable = aiService.isAvailable();

    log.info(
        "Pipeline availability check - Whisper: {}, Claude: {}", whisperAvailable, claudeAvailable);

    return whisperAvailable && claudeAvailable;
  }

  /**
   * Создает новый InterviewAnswer и сохраняет его в базу данных
   *
   * @param interviewId ID интервью
   * @param questionId ID вопроса
   * @param rawTranscription сырой транскрибированный текст
   * @param formattedText отформатированный текст
   * @return ID созданного InterviewAnswer
   */
  private Long createAndSaveInterviewAnswer(
      Long interviewId, Long questionId, String rawTranscription, String formattedText) {
    try {
      // Находим вопрос и получаем все связанные данные
      Question question =
          questionRepository
              .findById(questionId)
              .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

      Position position = question.getPosition();
      if (position == null) {
        throw new RuntimeException("Position not found for question: " + questionId);
      }

      // Находим интервью по ID
      Interview interview =
          interviewRepository
              .findById(interviewId)
              .orElseThrow(() -> new RuntimeException("Interview not found: " + interviewId));

      // Создаем InterviewAnswer с правильной связью с интервью
      InterviewAnswer answer =
          InterviewAnswer.builder()
              .interview(interview)
              .question(question)
              .rawTranscription(rawTranscription)
              .formattedTranscription(formattedText)
              .build();

      interviewAnswerRepository.save(answer);
      log.info(
          "Created InterviewAnswer with ID: {} for question: {} (interview: {}, position: {})",
          answer.getId(),
          questionId,
          interviewId,
          position.getTitle());

      return answer.getId();
    } catch (Exception e) {
      log.error(
          "Failed to create and save InterviewAnswer for interview ID: {} question ID: {}",
          interviewId,
          questionId,
          e);
      throw new RuntimeException("Failed to create and save InterviewAnswer", e);
    }
  }
}
