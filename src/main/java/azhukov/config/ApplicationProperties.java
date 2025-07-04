package azhukov.config;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства приложения. Использует @ConfigurationProperties для автоматического
 * связывания с application.yaml
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

  private Security security = new Security();
  private Cors cors = new Cors();
  private Pagination pagination = new Pagination();
  private File file = new File();
  private Ai ai = new Ai();
  private Anthropic anthropic = new Anthropic();
  private OpenAi openai = new OpenAi();

  // Константы для магических чисел
  public static final class Constants {
    public static final double DEFAULT_MIN_SCORE = 6.0;
    public static final int DEFAULT_ANSWER_TIME = 150;
    public static final int DEFAULT_QUESTIONS_COUNT = 5;
    public static final int DEFAULT_MAX_TOKENS = 1000;
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int DEFAULT_RETRY_DELAY_MS = 1000;
    public static final int DEFAULT_CACHE_SIZE = 1000;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_LANGUAGE = "Русский";
    public static final String DEFAULT_QUESTION_TYPE = "В основном хард-скиллы";
    public static final String DEFAULT_CHECK_TYPE = "Автоматическая проверка";
  }

  @Data
  public static class Security {
    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
      private String secret = "${JWT_SECRET:your-secret-key-here-make-it-long-and-secure}";
      private Duration expiration = Duration.ofHours(24);
    }
  }

  @Data
  public static class Cors {
    private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders =
        List.of(
            "*",
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers");
    private boolean allowCredentials = true;
  }

  @Data
  public static class Pagination {
    private int defaultPageSize = Constants.DEFAULT_PAGE_SIZE;
    private int maxPageSize = Constants.MAX_PAGE_SIZE;
  }

  @Data
  public static class File {
    private Upload upload = new Upload();

    @Data
    public static class Upload {
      private String maxSize = "10MB";
      private List<String> allowedTypes =
          List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "mp3", "wav", "mp4");
    }
  }

  @Data
  public static class Ai {
    private int maxQuestionsPerGeneration = 20;
    private Transcription transcription = new Transcription();

    @Data
    public static class Transcription {
      private int maxAudioDuration = 300; // 5 minutes
      private List<String> supportedFormats = List.of("mp3", "wav", "m4a");
      private Whisper whisper = new Whisper();

      @Data
      public static class Whisper {
        private String url = "${WHISPER_URL:http://localhost:9000}";
        private String modelSize = "base";
        private Duration timeout = Duration.ofSeconds(30);
        private int retryAttempts = 3;
      }
    }
  }

  @Data
  public static class Anthropic {
    private String apiKey = "${ANTHROPIC_API_KEY:}";
    private String model = "anthropic/claude-sonnet-4-20250522";
    private int maxTokens = Constants.DEFAULT_MAX_TOKENS;
    private double temperature = Constants.DEFAULT_TEMPERATURE;
  }

  @Data
  public static class OpenAi {
    private String apiKey = "${OPEAN_AI_API_KEY:}";
    private String model = "gpt-4";
    private int maxTokens = Constants.DEFAULT_MAX_TOKENS;
    private double temperature = Constants.DEFAULT_TEMPERATURE;
  }
}
