package azhukov.service.ai.elevenlabs.enums;

import lombok.Getter;

/** Эмоции для анализа в ElevenLabs Speech-to-Text Согласно актуальной документации */
@Getter
public enum ElevenLabsEmotion {
  NEUTRAL("neutral", "Нейтральная", "Neutral"),
  HAPPY("happy", "Радость", "Happy"),
  SAD("sad", "Грусть", "Sad"),
  ANGRY("angry", "Гнев", "Angry"),
  FEARFUL("fearful", "Страх", "Fearful"),
  DISGUSTED("disgusted", "Отвращение", "Disgusted"),
  SURPRISED("surprised", "Удивление", "Surprised"),
  EXCITED("excited", "Возбуждение", "Excited"),
  CALM("calm", "Спокойствие", "Calm"),
  CONFUSED("confused", "Растерянность", "Confused"),
  SATISFIED("satisfied", "Удовлетворение", "Satisfied"),
  DISAPPOINTED("disappointed", "Разочарование", "Disappointed"),
  CONFIDENT("confident", "Уверенность", "Confident"),
  NERVOUS("nervous", "Нервозность", "Nervous"),
  ENTHUSIASTIC("enthusiastic", "Энтузиазм", "Enthusiastic"),
  BORED("bored", "Скука", "Bored"),
  FRUSTRATED("frustrated", "Разочарование", "Frustrated"),
  AMUSED("amused", "Забава", "Amused"),
  CONCERNED("concerned", "Обеспокоенность", "Concerned"),
  OPTIMISTIC("optimistic", "Оптимизм", "Optimistic"),
  PESSIMISTIC("pessimistic", "Пессимизм", "Pessimistic");

  private final String code;
  private final String nameRu;
  private final String nameEn;

  ElevenLabsEmotion(String code, String nameRu, String nameEn) {
    this.code = code;
    this.nameRu = nameRu;
    this.nameEn = nameEn;
  }

  /** Получить эмоцию по коду */
  public static ElevenLabsEmotion fromCode(String code) {
    for (ElevenLabsEmotion emotion : values()) {
      if (emotion.getCode().equalsIgnoreCase(code)) {
        return emotion;
      }
    }
    throw new IllegalArgumentException("Unknown emotion code: " + code);
  }

  /** Получить эмоцию по коду с fallback на нейтральную */
  public static ElevenLabsEmotion fromCodeSafe(String code) {
    try {
      return fromCode(code);
    } catch (IllegalArgumentException e) {
      return NEUTRAL; // Fallback на нейтральную
    }
  }

  /** Проверить, является ли эмоция позитивной */
  public boolean isPositive() {
    return this == HAPPY
        || this == EXCITED
        || this == CALM
        || this == SATISFIED
        || this == CONFIDENT
        || this == ENTHUSIASTIC
        || this == AMUSED
        || this == OPTIMISTIC;
  }

  /** Проверить, является ли эмоция негативной */
  public boolean isNegative() {
    return this == SAD
        || this == ANGRY
        || this == FEARFUL
        || this == DISGUSTED
        || this == CONFUSED
        || this == DISAPPOINTED
        || this == NERVOUS
        || this == BORED
        || this == FRUSTRATED
        || this == CONCERNED
        || this == PESSIMISTIC;
  }
}
