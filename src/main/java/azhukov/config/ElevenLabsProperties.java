package azhukov.config;

import azhukov.service.ai.elevenlabs.enums.ElevenLabsLanguage;
import azhukov.service.ai.elevenlabs.enums.ElevenLabsModel;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурационные свойства для ElevenLabs API Заменяет Whisper для транскрибации аудио */
@Data
@ConfigurationProperties(prefix = "app.ai.transcription.elevenlabs")
public class ElevenLabsProperties {

  /** URL API ElevenLabs */
  private String apiUrl = "https://api.elevenlabs.io";

  /** API ключ ElevenLabs */
  private String apiKey;

  /** Таймаут для HTTP запросов */
  private Duration timeout = Duration.ofSeconds(60);

  /** Количество попыток повторного запроса при ошибке */
  private int retryAttempts = 3;

  /** Модель для STT */
  private ElevenLabsModel modelId = ElevenLabsModel.ELEVEN_MULTILINGUAL_V2;

  /** Язык для транскрибации */
  private ElevenLabsLanguage language = ElevenLabsLanguage.RUSSIAN;

  /** Температура для генерации (0.0 - 1.0) */
  private double temperature = 0.0;

  /** Поддерживаемые аудио форматы */
  private String[] supportedFormats = {"mp3", "wav", "m4a", "ogg", "flac", "aac", "webm"};

  /** Максимальный размер аудио файла в байтах (50MB) */
  private long maxFileSize = 50 * 1024 * 1024;

  /** Максимальная длительность аудио в секундах (5 минут) */
  private int maxDurationSeconds = 300;

  /** Включить детальное логирование */
  private boolean enableDetailedLogging = false;

  /** Включить кэширование результатов */
  private boolean enableCaching = true;

  /** Размер кэша в элементах */
  private int cacheSize = 1000;

  /** TTL кэша в секундах */
  private Duration cacheTtl = Duration.ofHours(24);

  /** Проверяет, что конфигурация корректна */
  public boolean isValid() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Возвращает метод аутентификации */
  public AuthMethod getAuthMethod() {
    if (apiKey != null && !apiKey.trim().isEmpty()) {
      return AuthMethod.API_KEY;
    } else {
      return AuthMethod.NONE;
    }
  }

  /** Методы аутентификации */
  public enum AuthMethod {
    API_KEY,
    NONE
  }
}
