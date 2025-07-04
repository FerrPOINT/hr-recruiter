package azhukov.config;

import azhukov.service.TranscriptionProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "transcription")
public class TranscriptionProperties {
  private TranscriptionProvider provider = TranscriptionProvider.ELEVENLABS;
}
