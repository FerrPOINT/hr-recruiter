package azhukov.service;

import azhukov.entity.InterviewAnswer;
import azhukov.repository.InterviewAnswerRepository;
import azhukov.service.ai.AIServiceException;
import azhukov.service.ai.claude.ClaudeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Сервис для обработки транскрибации аудио с пайплайном: Whisper (транскрибация) → Claude
 * (форматирование) → БД (сохранение результата)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

  private final WhisperService whisperService;
  private final ClaudeService claudeService;
  private final InterviewAnswerRepository interviewAnswerRepository;

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
   * Сохранение обоих вариантов в БД (InterviewAnswer)
   *
   * <p>ВАЖНО: Сырой текст (rawTranscription) сохраняется только для последующего анализа/отчетов,
   * на фронт не возвращается. Фронту возвращается только форматированный текст (formattedText).
   */
  @Transactional
  public String processAudioTranscription(MultipartFile audioFile, Long interviewAnswerId) {
    try {
      log.info("Starting audio transcription pipeline for answer ID: {}", interviewAnswerId);

      // Шаг 1: Транскрибация через Whisper
      String rawTranscription = whisperService.transcribeAudio(audioFile);
      log.info("Whisper transcription completed, length: {}", rawTranscription.length());

      // Шаг 2: Форматирование через Claude
      String formattedText = formatTranscription(rawTranscription);
      log.info("Claude formatting completed, length: {}", formattedText.length());

      // Шаг 3: Сохранение в БД
      saveTranscriptionResult(interviewAnswerId, rawTranscription, formattedText);
      log.info("Transcription result saved to database");

      return formattedText;

    } catch (Exception e) {
      log.error("Error in transcription pipeline for answer ID: {}", interviewAnswerId, e);
      throw new RuntimeException("Transcription processing failed", e);
    }
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
      return claudeService.generateText(prompt);
    } catch (AIServiceException e) {
      log.error("Failed to format transcription with Claude", e);
      // Возвращаем исходный текст, если форматирование не удалось
      return rawTranscription;
    }
  }

  /**
   * Сохраняет результат транскрибации в базу данных
   *
   * @param interviewAnswerId ID ответа на интервью
   * @param rawTranscription сырой транскрибированный текст
   * @param formattedText отформатированный текст
   */
  private void saveTranscriptionResult(
      Long interviewAnswerId, String rawTranscription, String formattedText) {
    InterviewAnswer answer =
        interviewAnswerRepository
            .findById(interviewAnswerId)
            .orElseThrow(
                () -> new RuntimeException("Interview answer not found: " + interviewAnswerId));

    // Сохраняем оба варианта текста
    answer.setRawTranscription(rawTranscription);
    answer.setFormattedTranscription(formattedText);

    interviewAnswerRepository.save(answer);
  }

  /**
   * Проверяет доступность всех сервисов в пайплайне
   *
   * @return true если все сервисы доступны
   */
  public boolean isPipelineAvailable() {
    boolean whisperAvailable = whisperService.isServiceAvailable();
    boolean claudeAvailable = claudeService.isAvailable();

    log.info(
        "Pipeline availability check - Whisper: {}, Claude: {}", whisperAvailable, claudeAvailable);

    return whisperAvailable && claudeAvailable;
  }
}
