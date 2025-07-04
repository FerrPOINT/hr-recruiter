package azhukov.service;

import azhukov.config.OpenRouterConfig;
import azhukov.service.ai.openrouter.OpenRouterService;
import azhukov.service.ai.openrouter.dto.OpenRouterMessage;
import azhukov.service.ai.openrouter.dto.OpenRouterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/** Локальный тест для OpenRouter сервиса. Использует реальный API ключ для тестирования. */
@Disabled("OpenRouter тест отключен - сервис работает корректно")
public class OpenRouterLocalTest {

  @Test
  void testOpenRouterTextFormatting() {
    System.out.println("=== ТЕСТИРОВАНИЕ OPENROUTER СЕРВИСА ===");

    // Создаем OpenRouterService с конфигурацией
    OpenRouterConfig config = new OpenRouterConfig();
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("WARNING: OPENROUTER_API_KEY не установлен. Тест будет пропущен.");
      return;
    }
    config.setApiKey(apiKey.trim());
    config.setModel("anthropic/claude-sonnet-4-20250522");
    config.setMaxTokens(1000);
    config.setApiUrl("https://openrouter.ai/api/v1/chat/completions");

    System.out.println("OpenRouter API KEY: " + config.getApiKey());
    System.out.println("OpenRouter API URL: " + config.getApiUrl());
    System.out.println("OpenRouter Model: " + config.getModel());

    // Выводим заголовки, которые будут использоваться
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + config.getApiKey());
    headers.set("HTTP-Referer", "https://github.com/azhukov/hr-recruiter-back");
    headers.set("X-Title", "HR Recruiter Backend");
    System.out.println("OpenRouter Headers: " + headers);

    OpenRouterService openRouterService =
        new OpenRouterService(config, new RestTemplate(), new ObjectMapper());

    // Тестируем форматирование текста
    String testText =
        "этотестовый текст дляпроверки форматрования он содержит несколько предложений мы хотим проверить как openrouter обрабатывает и форматирует текст";

    System.out.println("Исходный текст:");
    System.out.println(testText);
    System.out.println();

    // Тест 1: Простое форматирование
    System.out.println("=== ТЕСТ 1: Простое форматирование ===");
    testSimpleFormatting(openRouterService, testText);

    // Тест 2: Профессиональное форматирование
    System.out.println("=== ТЕСТ 2: Профессиональное форматирование ===");
    testProfessionalFormatting(openRouterService, testText);

    // Тест 3: Краткое форматирование
    System.out.println("=== ТЕСТ 3: Краткое форматирование ===");
    testConciseFormatting(openRouterService, testText);

    // Тест 4: Генерация вопросов для собеседования
    System.out.println("=== ТЕСТ 4: Генерация вопросов для собеседования ===");
    testInterviewQuestions(openRouterService);
  }

  private void testSimpleFormatting(OpenRouterService openRouterService, String text) {
    String result =
        openRouterService.generateText("Отформатируй этот текст красиво и читаемо: " + text);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  private void testProfessionalFormatting(OpenRouterService openRouterService, String text) {
    String prompt = "Перепиши этот текст в профессиональном стиле для деловой переписки: " + text;
    String result = openRouterService.generateText(prompt);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  private void testConciseFormatting(OpenRouterService openRouterService, String text) {
    String prompt = "Сократи этот текст, сохранив основную мысль: " + text;
    String result = openRouterService.generateText(prompt);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  private void testInterviewQuestions(OpenRouterService openRouterService) {
    String jobDescription =
        "Java Developer\n\n"
            + "Требования:\n"
            + "- Опыт работы с Java 8+ и Spring Framework\n"
            + "- Знание SQL и работы с базами данных\n"
            + "- Опыт работы с Git и CI/CD\n"
            + "- Умение работать в команде\n\n"
            + "Обязанности:\n"
            + "- Разработка новых функций\n"
            + "- Поддержка существующего кода\n"
            + "- Участие в code review\n"
            + "- Работа с командой разработки";

    List<String> questions =
        openRouterService.generateInterviewQuestions(
            jobDescription, 5, List.of("технические", "поведенческие", "ситуационные"));

    System.out.println("Сгенерированные вопросы:");
    for (int i = 0; i < questions.size(); i++) {
      System.out.println((i + 1) + ". " + questions.get(i));
    }
    System.out.println();
  }

  @Test
  void testDirectOpenRouterAPI() {
    System.out.println("=== ТЕСТИРОВАНИЕ ПРЯМОГО API OPENROUTER ===");

    RestTemplate restTemplate = new RestTemplate();

    // Настраиваем заголовки
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("WARNING: OPENROUTER_API_KEY не установлен. Тест будет пропущен.");
      return;
    }
    headers.set("Authorization", "Bearer " + apiKey);
    headers.set("HTTP-Referer", "https://github.com/azhukov/hr-recruiter-back");
    headers.set("X-Title", "HR Recruiter Backend");

    // Создаем запрос
    OpenRouterRequest request = new OpenRouterRequest();
    request.setModel("anthropic/claude-sonnet-4-20250522");
    request.setMaxTokens(100);
    request.setMessages(List.of(OpenRouterMessage.userMessage("Привет! Как дела?")));

    HttpEntity<OpenRouterRequest> requestEntity = new HttpEntity<>(request, headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "https://openrouter.ai/api/v1/chat/completions",
            HttpMethod.POST,
            requestEntity,
            Map.class);

    System.out.println("Статус ответа: " + response.getStatusCode());
    System.out.println("Тело ответа: " + response.getBody());
  }
}
