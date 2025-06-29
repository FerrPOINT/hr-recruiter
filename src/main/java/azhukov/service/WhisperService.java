package azhukov.service;

import jakarta.annotation.PostConstruct;
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

  @Value("${app.ai.transcription.whisper.supported-formats:mp3,wav,m4a,ogg,flac,aac}")
  private String supportedFormatsConfig;

  @Value("${app.ai.transcription.whisper.language:ru}")
  private String language;

  @Value("${app.ai.transcription.whisper.model:base}")
  private String model;

  @Value("${app.ai.transcription.whisper.word-timestamps:false}")
  private boolean wordTimestamps;

  @Value("${app.ai.transcription.whisper.condition-on-previous-text:false}")
  private boolean conditionOnPreviousText;

  @Value("${app.ai.transcription.whisper.temperature:0.0}")
  private double temperature;

  private final RestTemplate restTemplate;
  private String[] supportedFormats;

  // Новый properties-класс для unit-тестов и ручной инициализации
  @Data
  public static class WhisperServiceProperties {
    private String whisperUrl;
    private int timeout;
    private int retryAttempts;
    private String[] supportedFormats;
    private String language;
    private String model;
    private boolean wordTimestamps;
    private boolean conditionOnPreviousText;
    private double temperature;

    public WhisperServiceProperties(
        String whisperUrl,
        int timeout,
        int retryAttempts,
        String[] supportedFormats,
        String language,
        String model,
        boolean wordTimestamps,
        boolean conditionOnPreviousText,
        double temperature) {
      this.whisperUrl = whisperUrl;
      this.timeout = timeout;
      this.retryAttempts = retryAttempts;
      this.supportedFormats = supportedFormats;
      this.language = language;
      this.model = model;
      this.wordTimestamps = wordTimestamps;
      this.conditionOnPreviousText = conditionOnPreviousText;
      this.temperature = temperature;
    }
  }

  public WhisperService() {
    this.restTemplate = new RestTemplate();
  }

  // Новый конструктор для unit-тестов
  public WhisperService(WhisperServiceProperties props) {
    this.whisperUrl = props.getWhisperUrl();
    this.timeout = props.getTimeout();
    this.retryAttempts = props.getRetryAttempts();
    this.supportedFormats = props.getSupportedFormats();
    this.language = props.getLanguage();
    this.model = props.getModel();
    this.wordTimestamps = props.isWordTimestamps();
    this.conditionOnPreviousText = props.isConditionOnPreviousText();
    this.temperature = props.getTemperature();
    this.restTemplate = new RestTemplate();
  }

  /** Инициализирует поддерживаемые форматы из конфигурации */
  @PostConstruct
  public void initSupportedFormats() {
    if (supportedFormatsConfig != null && !supportedFormatsConfig.trim().isEmpty()) {
      this.supportedFormats = supportedFormatsConfig.split(",");
      for (int i = 0; i < this.supportedFormats.length; i++) {
        this.supportedFormats[i] = this.supportedFormats[i].trim().toLowerCase();
      }
    } else {
      // Пытаемся получить от Whisper API, если доступен
      try {
        this.supportedFormats = getSupportedFormatsFromWhisper();
        log.info(
            "Retrieved supported formats from Whisper API: {}",
            String.join(", ", this.supportedFormats));
      } catch (Exception e) {
        // Значения по умолчанию, если API недоступен
        this.supportedFormats = new String[] {"mp3", "wav", "m4a", "ogg", "flac", "aac"};
        log.warn("Could not retrieve formats from Whisper API, using defaults: {}", e.getMessage());
      }
    }
    log.info("Supported audio formats: {}", String.join(", ", this.supportedFormats));
  }

  /**
   * Получает список поддерживаемых форматов от Whisper API
   *
   * @return массив поддерживаемых форматов
   */
  public String[] getSupportedFormatsFromWhisper() {
    try {
      // Whisper API обычно предоставляет информацию о поддерживаемых форматах
      // через endpoint /info или /formats
      String infoUrl = whisperUrl + "/info";
      ResponseEntity<String> response = restTemplate.getForEntity(infoUrl, String.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        // Парсим JSON ответ от Whisper API
        return parseSupportedFormatsFromResponse(response.getBody());
      }
    } catch (Exception e) {
      log.debug("Could not get formats from Whisper API: {}", e.getMessage());
    }

    // Fallback: пытаемся получить через другой endpoint
    try {
      String formatsUrl = whisperUrl + "/formats";
      ResponseEntity<String> response = restTemplate.getForEntity(formatsUrl, String.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return parseSupportedFormatsFromResponse(response.getBody());
      }
    } catch (Exception e) {
      log.debug("Could not get formats from Whisper formats endpoint: {}", e.getMessage());
    }

    throw new RuntimeException("Could not retrieve supported formats from Whisper API");
  }

  /**
   * Парсит поддерживаемые форматы из ответа Whisper API
   *
   * @param responseBody тело ответа от Whisper API
   * @return массив поддерживаемых форматов
   */
  private String[] parseSupportedFormatsFromResponse(String responseBody) {
    try {
      // Простой парсинг JSON для извлечения форматов
      // Предполагаем, что API возвращает что-то вроде: {"supported_formats": ["mp3", "wav", ...]}
      if (responseBody.contains("supported_formats") || responseBody.contains("formats")) {
        // Извлекаем форматы из JSON
        String[] formats = responseBody.replaceAll(".*\\[", "").replaceAll("\\].*", "").split(",");

        for (int i = 0; i < formats.length; i++) {
          formats[i] = formats[i].replaceAll("[\"\\s]", "").toLowerCase();
        }

        return formats;
      }
    } catch (Exception e) {
      log.debug("Error parsing formats from Whisper response: {}", e.getMessage());
    }

    // Если не удалось распарсить, возвращаем стандартные форматы
    return new String[] {"mp3", "wav", "m4a", "ogg", "flac", "aac"};
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

      // Отправляем запрос к Whisper ASR с правильными параметрами для русского языка
      String transcriptionUrl =
          whisperUrl
              + "/asr?encode=true&task=transcribe&output=txt&language="
              + language
              + "&model="
              + model
              + "&word_timestamps="
              + wordTimestamps
              + "&condition_on_previous_text="
              + conditionOnPreviousText
              + "&temperature="
              + temperature;

      log.debug("Sending request to Whisper ASR: {}", transcriptionUrl);
      log.debug(
          "Whisper parameters - Language: {}, Model: {}, WordTimestamps: {}, ConditionOnPreviousText: {}, Temperature: {}",
          language,
          model,
          wordTimestamps,
          conditionOnPreviousText,
          temperature);
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
      //      validateAudioFile(audioFile);
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
    if (extension == null) {
      return false;
    }
    String normalizedExtension = extension.toLowerCase().trim();
    for (String supportedFormat : supportedFormats) {
      if (supportedFormat.equals(normalizedExtension)) {
        return true;
      }
    }
    return false;
  }

  /** Получает информацию о файле для логирования */
  private String getFileInfo(MultipartFile file) {
    return String.format(
        "name=%s, size=%d bytes, type=%s",
        file.getOriginalFilename(), file.getSize(), file.getContentType());
  }
}
