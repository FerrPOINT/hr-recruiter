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
}
