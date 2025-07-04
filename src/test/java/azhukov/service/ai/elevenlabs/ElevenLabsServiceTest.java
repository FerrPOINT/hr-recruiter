package azhukov.service.ai.elevenlabs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import azhukov.config.ElevenLabsProperties;
import azhukov.service.ai.elevenlabs.dto.ElevenLabsSTTResponse;
import azhukov.service.ai.elevenlabs.enums.ElevenLabsLanguage;
import azhukov.service.ai.elevenlabs.enums.ElevenLabsModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ElevenLabsServiceTest {

  @Mock private RestTemplate restTemplate;
  private ElevenLabsService elevenLabsService;
  private MockMultipartFile mockAudioFile;

  @BeforeEach
  void setUp() {
    ElevenLabsProperties props = new ElevenLabsProperties();
    props.setApiUrl("https://api.elevenlabs.io");
    props.setApiKey("test-api-key");
    props.setModelId(ElevenLabsModel.ELEVEN_MULTILINGUAL_V2);
    props.setLanguage(ElevenLabsLanguage.RUSSIAN);
    props.setTemperature(0.0);

    elevenLabsService = new ElevenLabsService(props, restTemplate, new ObjectMapper());

    // Создаем тестовый аудио файл
    byte[] audioData = "test audio data".getBytes();
    mockAudioFile = new MockMultipartFile("audio", "test.wav", "audio/wav", audioData);
  }

  @Test
  void testTranscribeAudio_Success() {
    ElevenLabsSTTResponse mockResponse = new ElevenLabsSTTResponse();
    mockResponse.setText("Тестовый транскрибированный текст");
    mockResponse.setConfidence(0.95);
    mockResponse.setLanguage("ru");

    ResponseEntity<ElevenLabsSTTResponse> responseEntity =
        new ResponseEntity<>(mockResponse, HttpStatus.OK);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ElevenLabsSTTResponse.class)))
        .thenReturn(responseEntity);

    String result = elevenLabsService.transcribeAudio(mockAudioFile);
    assertNotNull(result);
    assertEquals("Тестовый транскрибированный текст", result);
    verify(restTemplate, times(1))
        .exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ElevenLabsSTTResponse.class));
  }

  @Test
  void testTranscribeAudio_EmptyResponse() {
    ElevenLabsSTTResponse mockResponse = new ElevenLabsSTTResponse();
    mockResponse.setText("");
    mockResponse.setConfidence(0.0);
    ResponseEntity<ElevenLabsSTTResponse> responseEntity =
        new ResponseEntity<>(mockResponse, HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ElevenLabsSTTResponse.class)))
        .thenReturn(responseEntity);
    String result = elevenLabsService.transcribeAudio(mockAudioFile);
    assertEquals("", result);
  }

  @Test
  void testTranscribeAudio_HttpError() {
    ResponseEntity<ElevenLabsSTTResponse> responseEntity =
        new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ElevenLabsSTTResponse.class)))
        .thenReturn(responseEntity);
    assertThrows(RuntimeException.class, () -> elevenLabsService.transcribeAudio(mockAudioFile));
  }

  @Test
  void testIsServiceAvailable_Success() {
    ResponseEntity<String> responseEntity = new ResponseEntity<>("voices", HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(responseEntity);
    boolean result = elevenLabsService.isServiceAvailable();
    assertTrue(result);
  }

  @Test
  void testIsServiceAvailable_Failure() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RuntimeException("Connection failed"));
    boolean result = elevenLabsService.isServiceAvailable();
    assertFalse(result);
  }
}
