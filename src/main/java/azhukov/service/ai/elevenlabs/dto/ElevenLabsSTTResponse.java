package azhukov.service.ai.elevenlabs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * DTO для ответа от ElevenLabs Speech-to-Text API Согласно актуальной документации:
 * https://docs.elevenlabs.io/api-reference/speech-to-text
 */
@Data
public class ElevenLabsSTTResponse {

  /** Распознанный текст */
  private String text;

  /** Уверенность в распознавании (0.0 - 1.0) */
  private Double confidence;

  /** Определенный язык (ISO 639-1 код) */
  private String language;

  /** Сегменты с временными метками (если включены) */
  private List<Segment> segments;

  /** Временные метки слов (если включены) */
  @JsonProperty("word_timestamps")
  private List<WordTimestamp> wordTimestamps;

  /** Временные метки предложений (если включены) */
  @JsonProperty("sentence_timestamps")
  private List<SentenceTimestamp> sentenceTimestamps;

  /** Анализ эмоций (если включен) */
  @JsonProperty("emotion_analysis")
  private EmotionAnalysis emotionAnalysis;

  /** Информация о говорящем (если включено) */
  @JsonProperty("speaker_info")
  private SpeakerInfo speakerInfo;

  /** Сегмент аудио с временными метками */
  @Data
  public static class Segment {
    private Double start;
    private Double end;
    private String text;
    private Double confidence;
  }

  /** Временная метка слова */
  @Data
  public static class WordTimestamp {
    private String word;
    private Double start;
    private Double end;
    private Double confidence;
  }

  /** Временная метка предложения */
  @Data
  public static class SentenceTimestamp {
    private String sentence;
    private Double start;
    private Double end;
    private Double confidence;
  }

  /** Анализ эмоций */
  @Data
  public static class EmotionAnalysis {
    private String primaryEmotion;
    private Double primaryConfidence;
    private List<EmotionScore> emotionScores;

    @Data
    public static class EmotionScore {
      private String emotion;
      private Double score;
    }
  }

  /** Информация о говорящем */
  @Data
  public static class SpeakerInfo {
    private String speakerId;
    private Double confidence;
    private String gender;
    private Integer age;
  }
}
