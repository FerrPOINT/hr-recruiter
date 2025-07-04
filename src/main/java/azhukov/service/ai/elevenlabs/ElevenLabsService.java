package azhukov.service.ai.elevenlabs;

import azhukov.config.ElevenLabsProperties;
import azhukov.service.ai.elevenlabs.dto.ElevenLabsSTTResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Сервис для интеграции с ElevenLabs API Заменяет Whisper для транскрибации аудио Согласно
 * актуальной документации: https://docs.elevenlabs.io/api-reference/speech-to-text
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ElevenLabsService {

  private final ElevenLabsProperties properties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Транскрибирует аудио файл в текст используя ElevenLabs STT
   *
   * @param audioFile аудио файл для транскрибации
   * @return транскрибированный текст
   */
  public String transcribeAudio(MultipartFile audioFile) {
    long startTime = System.currentTimeMillis();

    try {
      log.info(
          "Starting ElevenLabs STT transcription for file: {} ({} bytes)",
          audioFile.getOriginalFilename(),
          audioFile.getSize());

      // Подготавливаем multipart данные согласно официальной документации ElevenLabs
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

      // Добавляем MultipartFile напрямую - Spring должен правильно его сериализовать
      body.add("audio", audioFile);

      log.info("Sending multipart request to ElevenLabs STT with {} parts", body.size());
      log.info(
          "Audio file: name={}, size={}, content-type={}",
          audioFile.getOriginalFilename(),
          audioFile.getSize(),
          audioFile.getContentType());

      // Добавляем параметры запроса согласно документации
      body.add("model_id", properties.getModelId().getModelId());
      body.add("language_code", properties.getLanguage().getCode());
      body.add("temperature", properties.getTemperature());

      // Настраиваем заголовки с аутентификацией
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      // Выбираем метод аутентификации
      switch (properties.getAuthMethod()) {
        case API_KEY:
          headers.set("xi-api-key", properties.getApiKey());
          break;
        default:
          throw new RuntimeException("No valid authentication method configured: API_KEY required");
      }

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      // Отправляем запрос к ElevenLabs STT API
      String transcriptionUrl = properties.getApiUrl() + "/v1/speech-to-text";

      log.info("Sending request to ElevenLabs STT: {}", transcriptionUrl);
      log.info(
          "ElevenLabs parameters - Model: {}, Language: {}, Temperature: {}, File: {} ({} bytes)",
          properties.getModelId().getModelId(),
          properties.getLanguage().getCode(),
          properties.getTemperature(),
          audioFile.getOriginalFilename(),
          audioFile.getSize());

      // Отладочная информация о multipart данных
      log.info("Multipart body contains {} parts", body.size());
      for (String key : body.keySet()) {
        Object value = body.getFirst(key);
        log.info(
            "Body part '{}': {} (type: {})",
            key,
            value,
            value != null ? value.getClass().getSimpleName() : "null");
      }

      ResponseEntity<ElevenLabsSTTResponse> response =
          restTemplate.exchange(
              transcriptionUrl, HttpMethod.POST, requestEntity, ElevenLabsSTTResponse.class);

      long processingTime = System.currentTimeMillis() - startTime;

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        ElevenLabsSTTResponse sttResponse = response.getBody();
        String transcription = sttResponse.getText();

        if (transcription != null && !transcription.trim().isEmpty()) {
          transcription = transcription.trim();
          log.info(
              "ElevenLabs STT transcription completed successfully in {} ms, result length: {}, confidence: {}",
              processingTime,
              transcription.length(),
              sttResponse.getConfidence());
          return transcription;
        } else {
          log.warn("ElevenLabs STT returned 200 OK but empty transcription");
          return "";
        }
      } else {
        String errorBody = response.hasBody() ? response.getBody().toString() : "<no body>";
        log.error(
            "Failed to transcribe audio: HTTP {} in {} ms, body: {}",
            response.getStatusCode(),
            processingTime,
            errorBody);
        throw new RuntimeException(
            "Failed to transcribe audio: " + response.getStatusCode() + ", body: " + errorBody);
      }

    } catch (Exception e) {
      long processingTime = System.currentTimeMillis() - startTime;
      log.error(
          "Error during ElevenLabs STT transcription (took {} ms): {}",
          processingTime,
          e.getMessage(),
          e);
      throw new RuntimeException("ElevenLabs STT transcription failed: " + e.getMessage(), e);
    }
  }

  /**
   * Проверяет доступность ElevenLabs API
   *
   * @return true если сервис доступен
   */
  public boolean isServiceAvailable() {
    try {
      String healthUrl = properties.getApiUrl() + "/v1/voices";
      HttpHeaders headers = new HttpHeaders();

      // Выбираем метод аутентификации
      switch (properties.getAuthMethod()) {
        case API_KEY:
          headers.set("xi-api-key", properties.getApiKey());
          break;
        default:
          log.warn("No valid authentication method configured");
          return false;
      }

      HttpEntity<String> requestEntity = new HttpEntity<>(headers);
      ResponseEntity<String> response =
          restTemplate.exchange(healthUrl, HttpMethod.GET, requestEntity, String.class);

      return response.getStatusCode() == HttpStatus.OK;
    } catch (Exception e) {
      log.warn("ElevenLabs API is not available: {}", e.getMessage());
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
      log.info(
          "Starting ElevenLabs audio processing for file: {}", audioFile.getOriginalFilename());

      // Валидация файла
      validateAudioFile(audioFile);
      log.info("Audio file validated: {} bytes", audioFile.getSize());

      // Транскрибация
      long transcriptionStart = System.currentTimeMillis();
      String transcription = transcribeAudio(audioFile);
      long transcriptionTime = System.currentTimeMillis() - transcriptionStart;

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "ElevenLabs audio processing completed in {} ms (transcription: {} ms)",
          totalTime,
          transcriptionTime);

      return transcription;

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("Error in ElevenLabs audio processing (took {} ms)", totalTime, e);
      throw new RuntimeException("ElevenLabs audio processing failed", e);
    }
  }

  /** Валидирует аудио файл */
  private void validateAudioFile(MultipartFile audioFile) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is empty or null");
    }

    // Проверка размера файла
    if (audioFile.getSize() > properties.getMaxFileSize()) {
      throw new IllegalArgumentException(
          String.format(
              "Audio file too large: %d bytes (max: %d bytes)",
              audioFile.getSize(), properties.getMaxFileSize()));
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

    if (properties.isEnableDetailedLogging()) {
      log.info(
          "Audio file validation passed: {} ({} bytes, {})",
          originalFilename,
          audioFile.getSize(),
          contentType);
    }
  }

  /** Проверяет поддерживаемые аудио форматы */
  private boolean isSupportedAudioFormat(String extension) {
    if (extension == null) {
      return false;
    }
    String normalizedExtension = extension.toLowerCase().trim();

    return Arrays.asList(properties.getSupportedFormats()).contains(normalizedExtension);
  }
}
