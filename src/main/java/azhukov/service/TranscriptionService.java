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
import java.time.LocalDateTime;
import java.util.List;
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
  private final InterviewService interviewService;

  private static final String FORMATTING_PROMPT =
      """
                    Ты форматируешь транскрибированный текст на русском языке. ВАЖНО: возвращай ТОЛЬКО отформатированный текст, БЕЗ комментариев, примечаний или объяснений.

                    КРИТИЧЕСКИ ВАЖНЫЕ ПРАВИЛА:
                    - Текст на русском языке
                    - НИКОГДА не заменяй русские слова на английские
                    - НИКОГДА не "исправляй" русские названия, имена, термины
                    - НИКОГДА не переводишь русские слова на английский
                    - Сохраняй ВСЕ русские слова точно как они есть
                    - Если слово звучит как русское - оставляй его русским
                    - Если слово звучит как английское - оставляй его английским
                    - НЕ угадывай и НЕ предполагай правильное написание

                    ПРАВИЛА ФОРМАТИРОВАНИЯ:
                    - Исправляй только очевидные ошибки транскрибации (пунктуация, заглавные буквы)
                    - НЕ добавляй слова, которых нет в оригинале
                    - НЕ изменяй смысл сказанного
                    - Сохраняй разговорный стиль
                    - Убирай только явные артефакты (повторы, междометия типа "э-э", "м-м")
                    - Всегда начинай предложение с заглавной буквы (даже если это одно слово)
                    - НИКОГДА не добавляй комментарии в скобках или примечания
                    - НИКОГДА не объясняй что ты делал

                    ПРИМЕРЫ (возвращай ТОЛЬКО правую часть):
                    "э-э ну я работал в компании э-э как её там да в общем занимался разработкой" → "Ну я работал в компании, в общем занимался разработкой"
                    "давайте поговорим о вашем опыте работы" → "Давайте поговорим о вашем опыте работы"
                    "я слушаю музыку группы Heart of Kaza" → "Я слушаю музыку группы Heart of Kaza"
                    "да" → "Да"
                    "нет" → "Нет"
                    "java" → "Java"
                    "spring boot" → "Spring Boot"
                    "heart of kaza" → "Heart of Kaza"
                    "копночная музыка" → "Копночная музыка"
                    "я работал с джава" → "Я работал с джава" (НЕ исправляй на "Java")
                    "использовал спринг" → "Использовал спринг" (НЕ исправляй на "Spring")
                    "делал фронтенд на реакте" → "Делал фронтенд на реакте" (НЕ исправляй на "React")

                    Отформатируй следующий текст (верни ТОЛЬКО результат):
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

      // ВРЕМЕННО ОТКЛЮЧЕНО: Claude форматирование
      // long claudeStart = System.currentTimeMillis();
      // String formattedText = formatTranscription(rawTranscription);
      // long claudeTime = System.currentTimeMillis() - claudeStart;

      // Используем сырой текст как отформатированный
      String formattedText = rawTranscription;
      long claudeTime = 0;

      log.info(
          "Claude formatting SKIPPED (using raw text), length: {} chars", formattedText.length());

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
          "Total transcription pipeline completed successfully in {} ms (Whisper: {} ms, Claude: SKIPPED, DB: {} ms)",
          totalTime,
          whisperTime,
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

      // Проверяем, это первый вопрос?
      List<InterviewAnswer> existingAnswers =
          interviewAnswerRepository.findByInterviewId(interviewId);
      boolean isFirstQuestion = existingAnswers.isEmpty();

      if (isFirstQuestion) {
        // Это первый вопрос - начинаем интервью
        log.info("First question for interview: {}, starting interview", interviewId);
        interview.setStatus(Interview.Status.IN_PROGRESS);

        // Время начала = текущее время
        LocalDateTime startTime = LocalDateTime.now();
        interview.setStartedAt(startTime);

        interviewRepository.save(interview);
        log.info("Interview {} started at: {}", interviewId, startTime);
      }

      // Создаем InterviewAnswer с правильной связью с интервью
      InterviewAnswer answer =
          InterviewAnswer.builder()
              .interview(interview)
              .question(question)
              .rawTranscription(rawTranscription)
              .formattedTranscription(formattedText)
              .answerText(formattedText)
              .build();

      interviewAnswerRepository.save(answer);
      log.info(
          "Created InterviewAnswer with ID: {} for question: {} (interview: {}, position: {})",
          answer.getId(),
          questionId,
          interviewId,
          position.getTitle());

      // Дополнительная проверка сохранения
      InterviewAnswer savedAnswer = interviewAnswerRepository.findById(answer.getId()).orElse(null);
      if (savedAnswer != null) {
        log.info(
            "InterviewAnswer {} successfully saved and retrieved from database", answer.getId());
      } else {
        log.error("InterviewAnswer {} was not found in database after save!", answer.getId());
      }

      // Проверяем, это последний вопрос?
      int totalQuestions = position.getQuestionsCount();
      long answeredQuestions = interviewAnswerRepository.countByInterviewId(interviewId);

      log.info(
          "Interview {}: answered {}/{} questions", interviewId, answeredQuestions, totalQuestions);

      if (answeredQuestions >= totalQuestions) {
        // Это последний ответ - завершаем собеседование атомарно
        log.info(
            "Last answer submitted for interview: {}, finishing interview (atomic)", interviewId);
        interview.setStatus(Interview.Status.FINISHED); // или нужный финальный статус
        interview.setFinishedAt(LocalDateTime.now());
        interviewRepository.save(interview);
        log.info("Interview {} marked as FINISHED", interviewId);
      }

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
