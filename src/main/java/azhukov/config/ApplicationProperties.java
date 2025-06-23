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

  @Data
  public static class Security {
    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
      private String secret = "your-secret-key-here-make-it-long-and-secure";
      private Duration expiration = Duration.ofHours(24);
    }
  }

  @Data
  public static class Cors {
    private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private boolean allowCredentials = true;
  }

  @Data
  public static class Pagination {
    private int defaultPageSize = 20;
    private int maxPageSize = 100;
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
        private String url = "http://localhost:9000";
        private String modelSize = "base";
        private Duration timeout = Duration.ofSeconds(30);
        private int retryAttempts = 3;
      }
    }
  }

  @Data
  public static class Anthropic {
    private String apiKey;
    private String model = "claude-3-sonnet-20240229";
    private int maxTokens = 1000;
    private double temperature = 0.7;
  }

  @Data
  public static class OpenAi {
    private String apiKey;
    private String model = "gpt-4";
    private int maxTokens = 1000;
    private double temperature = 0.7;
  }
}
