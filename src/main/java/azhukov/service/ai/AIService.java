package azhukov.service.ai;

import java.util.List;

/**
 * Интерфейс для AI сервисов. Следует принципу Interface Segregation (SOLID) - клиенты не должны
 * зависеть от методов, которые они не используют.
 *
 * @author AI Team
 * @version 1.0
 */
public interface AIService {

  /**
   * Отправляет текстовый запрос к AI и получает ответ.
   *
   * @param prompt Текст запроса для AI
   * @return Ответ от AI
   * @throws AIServiceException если произошла ошибка при обращении к AI
   */
  String generateText(String prompt) throws AIServiceException;

  /**
   * Генерирует список текстов на основе промпта.
   *
   * @param prompt Базовый промпт для генерации
   * @param count Количество элементов для генерации
   * @return Список сгенерированных текстов
   * @throws AIServiceException если произошла ошибка при обращении к AI
   */
  List<String> generateTextList(String prompt, int count) throws AIServiceException;

  /**
   * Проверяет доступность AI сервиса.
   *
   * @return true если сервис доступен, false в противном случае
   */
  boolean isAvailable();

  /**
   * Получает информацию о модели AI.
   *
   * @return Название модели AI
   */
  String getModelInfo();

  /**
   * Получает статистику использования AI сервиса.
   *
   * @return Статистика использования
   */
  AIUsageStats getUsageStats();

  /**
   * Генерирует вопросы для собеседования на основе описания позиции. Вся бизнес-логика и проверки
   * должны быть реализованы в сервисе, а не в контроллере.
   *
   * @param positionDescription Описание позиции
   * @param count Количество вопросов (по умолчанию 10)
   * @return Список вопросов
   */
  default List<String> generateQuestions(String positionDescription, int count)
      throws AIServiceException {
    // По умолчанию — просто делегируем в generateTextList
    String prompt =
        String.format(
            "Сгенерируй %d профессиональных вопросов для собеседования на основе следующего описания вакансии:\n\n%s\n\nВопросы должны быть разнообразными и покрывать технические навыки, soft skills и опыт работы. Верни только список вопросов, каждый с новой строки.",
            count, positionDescription);
    return generateTextList(prompt, count);
  }
}
