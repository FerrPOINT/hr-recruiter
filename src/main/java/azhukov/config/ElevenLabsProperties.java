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

  // Конфигурация для ElevenLabs Conversational AI
  /** ID агента по умолчанию */
  private String defaultAgentId;

  /** ID голоса по умолчанию */
  private String defaultVoiceId = "21m00Tcm4TlvDq8ikWAM"; // Rachel - профессиональный голос

  /** Язык по умолчанию */
  private String defaultLanguage = "ru";

  /** Промпт агента по умолчанию */
  private String defaultAgentPrompt =
      """
      Ты профессиональный HR-специалист, проводящий собеседование.
      Твоя задача - задавать вопросы кандидату и вести естественную беседу.

      Правила:
      1. Задавай только те вопросы, которые получаешь от системы
      2. Не придумывай свои вопросы
      3. Будь дружелюбным и профессиональным
      4. Если кандидат не отвечает, вежливо попроси повторить
      5. После каждого ответа переходи к следующему вопросу
      6. В конце поблагодари за участие

      Стиль общения: профессиональный, но дружелюбный
      """;

  /** Максимальная длительность сессии в минутах */
  private int maxSessionDurationMinutes = 60;

  /** Таймаут ожидания ответа в секундах */
  private int responseTimeoutSeconds = 30;

  /** Включить детекцию эмоций */
  private boolean enableEmotionDetection = true;

  /** Включить детекцию говорящего */
  private boolean enableSpeakerDetection = false;

  /** Включить временные метки */
  private boolean enableTimestamps = true;

  /** Включить анализ качества аудио */
  private boolean enableAudioQualityAnalysis = true;

  /** Настройки голоса: стабильность (0.0-1.0) */
  private double stability = 0.5;

  /** Настройки голоса: similarity boost (0.0-1.0) */
  private double similarityBoost = 0.75;

  /** Настройки голоса: style (0.0-1.0) */
  private double style = 0.0;

  /** Максимальная длительность ответа кандидата (сек) */
  private int maxAnswerDurationSeconds = 120;

  /** Минимальный порог confidence для принятия ответа */
  private double minConfidenceThreshold = 0.7;

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
