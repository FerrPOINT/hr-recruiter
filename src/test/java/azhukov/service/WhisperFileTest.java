package azhukov.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Disabled("Интеграционный тест - запускается отдельно")
public class WhisperFileTest {

  @Test
  void testTranscribeRealM4AFile() throws IOException {
    String filePath = "C:/Users/FerrPOINT/Downloads/rec2.wav";
    File file = new File(filePath);

    if (!file.exists()) {
      throw new IllegalStateException("Файл не найден: " + filePath);
    }

    System.out.println("Файл найден: " + filePath);
    System.out.println("Размер файла: " + file.length() + " байт");

    // Создаем MockMultipartFile
    MockMultipartFile audioFile =
        new MockMultipartFile("audio_file", file.getName(), "audio/wav", new FileInputStream(file));

    // Создаем WhisperService
    WhisperService.WhisperServiceProperties props =
        new WhisperService.WhisperServiceProperties(
            "http://localhost:9000",
            60000,
            3,
            new String[] {"mp3", "wav", "m4a", "ogg", "flac", "aac"},
            "ru",
            "medium",
            true,
            false,
            0.0);
    WhisperService whisperService = new WhisperService(props);

    // Проверяем доступность сервиса
    if (!whisperService.isServiceAvailable()) {
      throw new IllegalStateException("Whisper сервис недоступен!");
    }

    System.out.println("Whisper сервис доступен, начинаем транскрибацию...");

    String transcription = whisperService.transcribeAudio(audioFile);
    System.out.println("=== РЕЗУЛЬТАТ ТРАНСКРИБАЦИИ ===");
    System.out.println(transcription);
    System.out.println("=== КОНЕЦ РЕЗУЛЬТАТА ===");
  }

  @Test
  void testDirectWhisperAPI() throws IOException {
    String filePath = "C:/Users/FerrPOINT/Downloads/rec2.wav";
    File file = new File(filePath);

    if (!file.exists()) {
      throw new IllegalStateException("Файл не найден: " + filePath);
    }

    System.out.println("Тестируем прямой API запрос к Whisper...");

    RestTemplate restTemplate = new RestTemplate();

    // Подготавливаем multipart данные
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "audio_file",
        new ByteArrayResource(new FileInputStream(file).readAllBytes()) {
          @Override
          public String getFilename() {
            return file.getName();
          }
        });

    // Настраиваем заголовки
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    // Отправляем запрос с правильными параметрами
    String transcriptionUrl = "http://localhost:9000/asr?encode=true&task=transcribe&output=txt";

    ResponseEntity<String> response =
        restTemplate.exchange(transcriptionUrl, HttpMethod.POST, requestEntity, String.class);

    System.out.println("Статус ответа: " + response.getStatusCode());
    System.out.println("Заголовки ответа: " + response.getHeaders());
    System.out.println("Тело ответа: " + response.getBody());

    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      String transcription = response.getBody();
      System.out.println("=== РЕЗУЛЬТАТ ТРАНСКРИБАЦИИ ===");
      System.out.println(transcription);
      System.out.println("=== КОНЕЦ РЕЗУЛЬТАТА ===");
    }
  }
}
