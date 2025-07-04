package azhukov.service.ai.openai;

import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiSttService {

  private final OpenAiAudioTranscriptionModel audioTranscriptionModel;

  public String transcribeAudio(MultipartFile audioFile) {
    try {
      File tempFile = File.createTempFile("openai-upload-", "-" + audioFile.getOriginalFilename());
      audioFile.transferTo(tempFile);
      log.info(
          "[OpenAI] Audio file saved for debug: {} ({} bytes)",
          tempFile.getAbsolutePath(),
          tempFile.length());

      OpenAiAudioTranscriptionOptions options =
          OpenAiAudioTranscriptionOptions.builder()
              .model("whisper-1")
              .language("ru")
              .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
              .build();

      AudioTranscriptionPrompt prompt =
          new AudioTranscriptionPrompt(new FileSystemResource(tempFile), options);

      AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);

      if (response.getResults() == null || response.getResults().isEmpty()) {
        log.error("[OpenAI] No transcription results returned");
        throw new RuntimeException("No transcription results returned");
      }

      String transcription = response.getResults().get(0).toString();
      log.info("[OpenAI] Transcription completed, length: {} chars", transcription.length());
      return transcription;
    } catch (Exception e) {
      log.error("[OpenAI] Error during audio transcription", e);
      throw new RuntimeException("OpenAI transcription failed: " + e.getMessage(), e);
    }
  }
}
