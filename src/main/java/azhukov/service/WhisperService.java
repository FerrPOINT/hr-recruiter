package azhukov.service;

import java.io.IOException;
import java.util.Map;
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

  public WhisperService() {
    this.restTemplate = new RestTemplate();
  }

  /**
   * Транскрибирует аудио файл в текст
   *
   * @param audioFile аудио файл для транскрибации
   * @return транскрибированный текст
   */
  public String transcribeAudio(MultipartFile audioFile) {
    try {
      log.info("Starting audio transcription for file: {}", audioFile.getOriginalFilename());

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

      // Отправляем запрос к Whisper ASR
      String transcriptionUrl = whisperUrl + "/asr";
      ResponseEntity<Map> response =
          restTemplate.exchange(transcriptionUrl, HttpMethod.POST, requestEntity, Map.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        String transcription = (String) response.getBody().get("text");
        log.info("Audio transcription completed successfully");
        return transcription != null ? transcription.trim() : "";
      } else {
        log.error("Failed to transcribe audio: {}", response.getStatusCode());
        throw new RuntimeException("Failed to transcribe audio");
      }

    } catch (IOException e) {
      log.error("Error reading audio file", e);
      throw new RuntimeException("Error processing audio file", e);
    } catch (Exception e) {
      log.error("Error during audio transcription", e);
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
      String healthUrl = whisperUrl + "/health";
      ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
      return response.getStatusCode() == HttpStatus.OK;
    } catch (Exception e) {
      log.warn("Whisper ASR service is not available: {}", e.getMessage());
      return false;
    }
  }
}
