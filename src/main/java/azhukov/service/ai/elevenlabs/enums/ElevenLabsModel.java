package azhukov.service.ai.elevenlabs.enums;

import lombok.Getter;

/**
 * Доступные модели ElevenLabs для Speech-to-Text Согласно актуальной документации:
 * https://docs.elevenlabs.io/api-reference/speech-to-text
 */
@Getter
public enum ElevenLabsModel {

  /**
   * Многоязычная модель (поддерживает 29 языков) Рекомендуется для большинства случаев
   * использования
   */
  ELEVEN_MULTILINGUAL_V2("eleven_multilingual_v2", "Многоязычная модель v2", true),

  /** Английская модель с улучшенной точностью Только для английского языка */
  ELEVEN_ENGLISH_STS_V2("eleven_english_sts_v2", "Английская модель v2", false),

  /** Многоязычная модель v1 (устаревшая) Используется для обратной совместимости */
  ELEVEN_MULTILINGUAL_V1("eleven_multilingual_v1", "Многоязычная модель v1", true);

  private final String modelId;
  private final String description;
  private final boolean multilingual;

  ElevenLabsModel(String modelId, String description, boolean multilingual) {
    this.modelId = modelId;
    this.description = description;
    this.multilingual = multilingual;
  }

  /** Получить модель по ID */
  public static ElevenLabsModel fromModelId(String modelId) {
    for (ElevenLabsModel model : values()) {
      if (model.getModelId().equals(modelId)) {
        return model;
      }
    }
    throw new IllegalArgumentException("Unknown model ID: " + modelId);
  }

  /** Получить рекомендуемую модель для языка */
  public static ElevenLabsModel getRecommendedForLanguage(String languageCode) {
    if ("en".equalsIgnoreCase(languageCode)) {
      return ELEVEN_ENGLISH_STS_V2;
    } else {
      return ELEVEN_MULTILINGUAL_V2;
    }
  }

  /** Проверить, поддерживает ли модель язык */
  public boolean supportsLanguage(String languageCode) {
    if (this.multilingual) {
      return true; // Многоязычные модели поддерживают все языки
    } else {
      return "en".equalsIgnoreCase(languageCode); // Английская модель только для английского
    }
  }
}
