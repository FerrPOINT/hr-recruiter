package azhukov;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Тест для проверки корректности контроллеров. Все контроллеры должны реализовывать интерфейс *Api
 * и помечать публичные методы как @Override.
 */
public class ControllerValidationTest {

  @Test
  public void testAllControllersImplementApiInterfaces() throws IOException {
    System.out.println("🔍 Проверка контроллеров...");

    Path sourceDir = Paths.get("src/main/java");
    Path generatedApiDir = Paths.get("build/generated-sources/openapi/src/main/java/azhukov/api");
    List<String> issues = new ArrayList<>();

    // Подсчитываем количество API интерфейсов
    long apiInterfacesCount = 0;
    if (Files.exists(generatedApiDir)) {
      apiInterfacesCount =
          Files.walk(generatedApiDir)
              .filter(path -> path.toString().endsWith(".java"))
              .filter(path -> path.getFileName().toString().endsWith("Api.java"))
              .count();
    }

    // Подсчитываем количество контроллеров
    long controllersCount =
        Files.walk(sourceDir)
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> path.getFileName().toString().contains("Controller"))
            .filter(
                path -> {
                  try {
                    String content = Files.readString(path);
                    return content.contains("@RestController");
                  } catch (IOException e) {
                    return false;
                  }
                })
            .count();

    System.out.println("📊 Статистика:");
    System.out.println("  - API интерфейсов: " + apiInterfacesCount);
    System.out.println("  - Контроллеров: " + controllersCount);

    if (apiInterfacesCount != controllersCount) {
      issues.add(
          "Количество контроллеров ("
              + controllersCount
              + ") не соответствует количеству API интерфейсов ("
              + apiInterfacesCount
              + ")");
    }

    // Проверяем каждый контроллер
    Files.walk(sourceDir)
        .filter(path -> path.toString().endsWith(".java"))
        .filter(path -> path.getFileName().toString().contains("Controller"))
        .forEach(
            file -> {
              List<String> issuesInFile = checkController(file);
              issues.addAll(issuesInFile);
            });

    if (!issues.isEmpty()) {
      System.out.println("❌ Найдены проблемы в контроллерах:");
      issues.forEach(issue -> System.out.println("  - " + issue));
      fail("Контроллеры содержат ошибки: " + String.join(", ", issues));
    } else {
      System.out.println("✅ Все контроллеры корректны!");
    }
  }

  private List<String> checkController(Path file) {
    List<String> issues = new ArrayList<>();

    try {
      String content = Files.readString(file);

      // Проверяем, есть ли @RestController аннотация
      if (!content.contains("@RestController")) {
        return issues; // Пропускаем, если это не REST контроллер
      }

      System.out.println("📋 Проверяем: " + file.getFileName());

      // Проверяем, реализует ли класс интерфейс
      Pattern implementsPattern = Pattern.compile("implements\\s+(\\w+Api)");
      Matcher implementsMatch = implementsPattern.matcher(content);

      if (!implementsMatch.find()) {
        issues.add(file.getFileName() + ": Класс не реализует интерфейс API");
        return issues;
      }

      String interfaceName = implementsMatch.group(1);
      System.out.println("  📦 Реализует интерфейс: " + interfaceName);

      // Проверяем, что все методы интерфейса реализованы с @Override
      // Это упрощенная проверка - просто убеждаемся, что есть методы с @Override
      Pattern overridePattern = Pattern.compile("@Override");
      Matcher overrideMatcher = overridePattern.matcher(content);

      if (!overrideMatcher.find()) {
        issues.add(
            file.getFileName() + ": Контроллер должен реализовывать методы интерфейса с @Override");
      }

    } catch (IOException e) {
      issues.add(file.getFileName() + ": Ошибка чтения файла: " + e.getMessage());
    }

    return issues;
  }

  private int findLineNumber(String content, int position) {
    String beforePosition = content.substring(0, position);
    return beforePosition.split("\n").length;
  }
}
