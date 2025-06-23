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
 * Тест для проверки корректности контроллеров. Убеждается, что все методы в контроллерах помечены
 * как @Override, так как они должны реализовывать интерфейсы из OpenAPI.
 */
public class ControllerValidationTest {

  @Test
  public void testAllControllersImplementApiInterfaces() throws IOException {
    System.out.println("🔍 Проверка контроллеров...");

    Path sourceDir = Paths.get("src/main/java");
    List<String> issues = new ArrayList<>();

    // Ищем все Java файлы с "Controller" в названии
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

      // Ищем все публичные методы
      Pattern methodPattern =
          Pattern.compile(
              "(?:public|protected)\\s+(?:static\\s+)?(?:final\\s+)?(?:<[^>]+>\\s+)?(?:[\\w<>\\[\\]]+\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*\\{");
      Matcher methodMatcher = methodPattern.matcher(content);

      while (methodMatcher.find()) {
        int lineNumber = findLineNumber(content, methodMatcher.start());
        String methodName = methodMatcher.group(1);

        // Пропускаем конструкторы и служебные методы
        if (methodName.equals(file.getFileName().toString().replace(".java", ""))
            || methodName.equals("main")
            || methodName.equals("checkControllers")
            || methodName.equals("checkController")
            || methodName.equals("findLineNumber")) {
          continue;
        }

        // Проверяем, есть ли @Override перед методом
        int methodStart = methodMatcher.start();
        String beforeMethod = content.substring(0, methodStart);

        // Ищем @Override в последних 10 строках перед методом
        String[] lines = beforeMethod.split("\n");
        boolean hasOverride = false;
        int startIndex = Math.max(0, lines.length - 10);

        for (int i = startIndex; i < lines.length; i++) {
          String line = lines[i].trim();
          if (line.equals("@Override") || line.startsWith("@Override")) {
            hasOverride = true;
            break;
          }
        }

        if (!hasOverride) {
          issues.add(
              file.getFileName()
                  + ":"
                  + lineNumber
                  + ": Метод '"
                  + methodName
                  + "' должен быть помечен как @Override");
        }
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
