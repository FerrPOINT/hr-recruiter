package azhukov.service;

import azhukov.config.ClaudeConfig;
import azhukov.service.ai.claude.ClaudeService;
import azhukov.service.ai.claude.dto.ClaudeMessage;
import azhukov.service.ai.claude.dto.ClaudeRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Disabled("Claude тест отключен - используем OpenRouter")
public class ClaudeServiceTest {

  @Test
  void testClaudeTextFormatting() {
    System.out.println("=== ТЕСТИРОВАНИЕ CLAUDE СЕРВИСА ===");

    // Создаем ClaudeService с конфигурацией
    ClaudeConfig config = new ClaudeConfig();
    String apiKey = System.getenv("CLAUDE_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      apiKey = "v1-a82138f8985c68a0f6573cd443141753e3aa5173707959864c6b1a87452fe045";
    }
    config.setApiKey(apiKey.trim());
    config.setModel("claude-3-5-sonnet-20241022");
    config.setMaxTokens(1000);
    config.setApiUrl("https://api.anthropic.com/v1/messages");

    System.out.println("Claude API KEY: " + config.getApiKey());
    System.out.println("Claude API URL: " + config.getApiUrl());
    System.out.println("Claude Model: " + config.getModel());

    // Выводим заголовки, которые будут использоваться
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    headers.set("x-api-key", config.getApiKey());
    headers.set("anthropic-version", "2023-06-01");
    System.out.println("Claude Headers: " + headers);

    ClaudeService claudeService = new ClaudeService(config, new RestTemplate());

    // Тестируем форматирование текста
    String testText =
        "этотестовый текст дляпроверки форматрования он содержит несколько предложений мы хотим проверить как клод обрабатывает и форматирует текст";

    System.out.println("Исходный текст:");
    System.out.println(testText);
    System.out.println();

    // Тест 1: Простое форматирование
    System.out.println("=== ТЕСТ 1: Простое форматирование ===");
    testSimpleFormatting(claudeService, testText);

    // Тест 2: Профессиональное форматирование
    System.out.println("=== ТЕСТ 2: Профессиональное форматирование ===");
    testProfessionalFormatting(claudeService, testText);

    // Тест 3: Краткое форматирование
    System.out.println("=== ТЕСТ 3: Краткое форматирование ===");
    testConciseFormatting(claudeService, testText);
  }

  private void testSimpleFormatting(ClaudeService claudeService, String text) {
    String result =
        claudeService.generateText("Отформатируй этот текст красиво и читаемо: " + text);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  private void testProfessionalFormatting(ClaudeService claudeService, String text) {
    String prompt = "Перепиши этот текст в профессиональном стиле для деловой переписки: " + text;
    String result = claudeService.generateText(prompt);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  private void testConciseFormatting(ClaudeService claudeService, String text) {
    String prompt = "Сократи этот текст, сохранив основную мысль: " + text;
    String result = claudeService.generateText(prompt);
    System.out.println("Результат:");
    System.out.println(result);
    System.out.println();
  }

  @Test
  void testDirectClaudeAPI() {
    System.out.println("=== ТЕСТИРОВАНИЕ ПРЯМОГО API CLAUDE ===");

    RestTemplate restTemplate = new RestTemplate();

    // Настраиваем заголовки
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", System.getenv("CLAUDE_API_KEY")); // Используем переменную окружения
    headers.set("anthropic-version", "2023-06-01");

    // Создаем запрос
    ClaudeRequest request = new ClaudeRequest();
    request.setModel("claude-3-sonnet-20240229");
    request.setMaxTokens(1000);
    request.setMessages(List.of(ClaudeMessage.userMessage("Привет! Как дела?")));

    HttpEntity<ClaudeRequest> requestEntity = new HttpEntity<>(request, headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "https://api.anthropic.com/v1/messages", HttpMethod.POST, requestEntity, Map.class);

    System.out.println("Статус ответа: " + response.getStatusCode());
    System.out.println("Тело ответа: " + response.getBody());
  }
}
