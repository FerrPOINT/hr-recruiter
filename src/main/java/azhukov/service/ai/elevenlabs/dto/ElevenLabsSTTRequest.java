package azhukov.service.ai.elevenlabs.dto;

import azhukov.service.ai.elevenlabs.enums.ElevenLabsLanguage;
import azhukov.service.ai.elevenlabs.enums.ElevenLabsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO для запроса к ElevenLabs Speech-to-Text API Согласно актуальной документации:
 * https://docs.elevenlabs.io/api-reference/speech-to-text
 */
@Data
public class ElevenLabsSTTRequest {

  /** Модель для распознавания речи */
  @JsonProperty("model_id")
  private ElevenLabsModel modelId = ElevenLabsModel.ELEVEN_MULTILINGUAL_V2;

  /** Язык для распознавания */
  @JsonProperty("language_code")
  private ElevenLabsLanguage languageCode = ElevenLabsLanguage.RUSSIAN;

  /** Подсказка для улучшения точности распознавания Может содержать контекст или ключевые слова */
  private String prompt;

  /**
   * Температура для генерации (0.0 - 1.0) 0.0 = более точное распознавание, 1.0 = более креативное
   */
  private Double temperature = 0.0;

  /**
   * Ключевые слова для улучшения распознавания Массив слов, которые должны быть распознаны с
   * приоритетом
   */
  @JsonProperty("word_boost")
  private String[] wordBoost;

  /** Включить временные метки для каждого слова */
  @JsonProperty("word_timestamps")
  private Boolean wordTimestamps = false;

  /** Включить сегментацию по предложениям */
  @JsonProperty("sentence_timestamps")
  private Boolean sentenceTimestamps = false;

  /** Включить анализ эмоций */
  @JsonProperty("emotion_detection")
  private Boolean emotionDetection = false;

  /** Включить определение говорящего */
  @JsonProperty("speaker_detection")
  private Boolean speakerDetection = false;

  /**
   * Аудио данные передаются отдельно как multipart/form-data Этот класс используется только для
   * параметров запроса
   */
}
