package azhukov.service;

import java.io.IOException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class WhisperService {

  @Value("${app.ai.transcription.whisper.url}")
  private String whisperUrl;

  @Value("${app.ai.transcription.whisper.timeout:30000}")
  private int timeout;

  @Value("${app.ai.transcription.whisper.retry-attempts:3}")
  private int retryAttempts;

  private final RestTemplate restTemplate;

  // Новый properties-класс для unit-тестов и ручной инициализации
  @Data
  public static class WhisperServiceProperties {
    private String whisperUrl;
    private int timeout;
    private int retryAttempts;

    public WhisperServiceProperties(String whisperUrl, int timeout, int retryAttempts) {
      this.whisperUrl = whisperUrl;
      this.timeout = timeout;
      this.retryAttempts = retryAttempts;
    }
  }

  public WhisperService() {
    this.restTemplate = new RestTemplate();
  }

  // Новый конструктор для unit-тестов
  public WhisperService(WhisperServiceProperties props) {
    this.whisperUrl = props.whisperUrl;
    this.timeout = props.timeout;
    this.retryAttempts = props.retryAttempts;
    this.restTemplate = new RestTemplate();
  }

  /**
   * Транскрибирует аудио файл в текст с улучшенной обработкой ошибок
   *
   * @param audioFile аудио файл для транскрибации
   * @return транскрибированный текст
   */
  public String transcribeAudio(MultipartFile audioFile) {
    long startTime = System.currentTimeMillis();

    try {
      log.info(
          "Starting audio transcription for file: {} ({} bytes)",
          audioFile.getOriginalFilename(),
          audioFile.getSize());

      // Проверяем доступность сервиса перед отправкой
      if (!isServiceAvailable()) {
        throw new RuntimeException("Whisper ASR service is not available");
      }

      // Подготавливаем multipart данные
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add(
          "audio_file",
          new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
              return audioFile.getOriginalFilename();
            }
          });

      // Настраиваем заголовки
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      // Отправляем запрос к Whisper ASR с правильными параметрами
      String transcriptionUrl = whisperUrl + "/asr?encode=true&task=transcribe&output=txt";

      log.debug("Sending request to Whisper ASR: {}", transcriptionUrl);
      ResponseEntity<String> response =
          restTemplate.exchange(transcriptionUrl, HttpMethod.POST, requestEntity, String.class);

      long processingTime = System.currentTimeMillis() - startTime;

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        String transcription = response.getBody();
        log.info(
            "Audio transcription completed successfully in {} ms, result length: {}",
            processingTime,
            transcription != null ? transcription.length() : 0);
        return transcription != null ? transcription.trim() : "";
      } else {
        log.error(
            "Failed to transcribe audio: HTTP {} in {} ms",
            response.getStatusCode(),
            processingTime);
        throw new RuntimeException("Failed to transcribe audio: " + response.getStatusCode());
      }

    } catch (IOException e) {
      log.error("Error reading audio file: {}", e.getMessage(), e);
      throw new RuntimeException("Error processing audio file", e);
    } catch (Exception e) {
      long processingTime = System.currentTimeMillis() - startTime;
      log.error(
          "Error during audio transcription (took {} ms): {}", processingTime, e.getMessage(), e);
      throw new RuntimeException("Audio transcription failed", e);
    }
  }

  /**
   * Проверяет доступность Whisper ASR сервиса
   *
   * @return true если сервис доступен
   */
  public boolean isServiceAvailable() {
    try {
      String healthUrl = whisperUrl;
      ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
      return response.getStatusCode() == HttpStatus.OK;
    } catch (Exception e) {
      log.warn("Whisper ASR service is not available: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Полная обработка аудио файла с валидацией и логированием
   *
   * @param audioFile аудио файл для транскрибации
   * @return транскрибированный текст
   */
  public String processAudioFile(MultipartFile audioFile) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("Starting audio processing for file: {}", audioFile.getOriginalFilename());

      // Валидация файла
      validateAudioFile(audioFile);
      log.info("Audio file validated: {} bytes", audioFile.getSize());

      // Транскрибация
      long transcriptionStart = System.currentTimeMillis();
      String transcription = transcribeAudio(audioFile);
      long transcriptionTime = System.currentTimeMillis() - transcriptionStart;

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "Audio processing completed in {} ms (transcription: {} ms)",
          totalTime,
          transcriptionTime);

      return transcription;

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("Error in audio processing (took {} ms)", totalTime, e);
      throw new RuntimeException("Audio processing failed", e);
    }
  }

  /** Валидирует аудио файл с расширенными проверками */
  private void validateAudioFile(MultipartFile audioFile) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is empty or null");
    }

    // Проверка размера файла (50MB)
    long maxSize = 50 * 1024 * 1024;
    if (audioFile.getSize() > maxSize) {
      throw new IllegalArgumentException(
          String.format(
              "Audio file too large: %d bytes (max: %d bytes)", audioFile.getSize(), maxSize));
    }

    // Проверка типа файла
    String contentType = audioFile.getContentType();
    if (contentType == null || !contentType.startsWith("audio/")) {
      throw new IllegalArgumentException("Invalid audio file type: " + contentType);
    }

    // Проверка расширения файла
    String originalFilename = audioFile.getOriginalFilename();
    if (originalFilename != null) {
      String extension =
          originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
      if (!isSupportedAudioFormat(extension)) {
        throw new IllegalArgumentException("Unsupported audio format: " + extension);
      }
    }

    log.info(
        "Audio file validation passed: {} ({} bytes, {})",
        originalFilename,
        audioFile.getSize(),
        contentType);
  }

  /** Проверяет поддерживаемые аудио форматы */
  private boolean isSupportedAudioFormat(String extension) {
    return extension.matches("(mp3|wav|m4a|ogg|flac|aac)");
  }

  /** Получает информацию о файле для логирования */
  private String getFileInfo(MultipartFile file) {
    return String.format(
        "name=%s, size=%d bytes, type=%s",
        file.getOriginalFilename(), file.getSize(), file.getContentType());
  }
}
