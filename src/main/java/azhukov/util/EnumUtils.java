package azhukov.util;

import lombok.extern.slf4j.Slf4j;

/** Утилиты для работы с enum'ами. Предоставляет методы для безопасного маппинга строк в enum'ы. */
@Slf4j
public class EnumUtils {

  /**
   * Безопасно преобразует строку в enum с fallback значением.
   *
   * @param value Строковое значение
   * @param enumClass Класс enum'а
   * @param fallback Fallback значение
   * @param <T> Тип enum'а
   * @return Enum значение или fallback
   */
  public static <T extends Enum<T>> T safeValueOf(String value, Class<T> enumClass, T fallback) {
    if (value == null || value.trim().isEmpty()) {
      log.debug(
          "Empty value provided for enum {}, using fallback: {}",
          enumClass.getSimpleName(),
          fallback);
      return fallback;
    }

    try {
      return Enum.valueOf(enumClass, value.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid enum value '{}' for {}, using fallback: {}",
          value,
          enumClass.getSimpleName(),
          fallback);
      return fallback;
    }
  }

  /**
   * Безопасно преобразует строку в enum с дефолтным значением. Используется когда есть стандартное
   * дефолтное значение для enum'а.
   *
   * @param value Строковое значение
   * @param enumClass Класс enum'а
   * @param <T> Тип enum'а
   * @return Enum значение или null
   */
  public static <T extends Enum<T>> T safeValueOf(String value, Class<T> enumClass) {
    return safeValueOf(value, enumClass, null);
  }

  /**
   * Проверяет, является ли строка валидным значением enum'а.
   *
   * @param value Строковое значение
   * @param enumClass Класс enum'а
   * @param <T> Тип enum'а
   * @return true если значение валидно
   */
  public static <T extends Enum<T>> boolean isValidEnum(String value, Class<T> enumClass) {
    if (value == null || value.trim().isEmpty()) {
      return false;
    }

    try {
      Enum.valueOf(enumClass, value.toUpperCase());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Получает все значения enum'а в виде массива строк.
   *
   * @param enumClass Класс enum'а
   * @param <T> Тип enum'а
   * @return Массив строковых значений
   */
  public static <T extends Enum<T>> String[] getEnumValues(Class<T> enumClass) {
    T[] values = enumClass.getEnumConstants();
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].name();
    }
    return result;
  }
}
