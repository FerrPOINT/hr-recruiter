# AI-генерация вакансий - Руководство разработчика

## Обзор

Система AI-генерации вакансий позволяет автоматически создавать структуру вакансии на основе описания пользователя. AI возвращает минимальный набор полей, а остальные бизнес-поля устанавливаются системой по умолчанию.

## Архитектура

### Компоненты

1. **OpenRouterService** - основной сервис для работы с AI
2. **AiController** - REST API контроллер
3. **PositionGenerationResponse** - DTO для AI-ответа
4. **OpenAPI схемы** - контракты API

### Поток данных

```
Frontend → AiController → OpenRouterService → AI API → DTO → OpenAPI Model → Frontend
```

## API Endpoints

### POST /ai/generate-position

Генерирует структуру вакансии с помощью AI.

#### Запрос (PositionAiGenerationRequest)

```json
{
  "description": "Нужен Java разработчик для разработки микросервисов",
  "questionsCount": 5,
  "questionType": "hard"
}
```

#### Ответ (PositionAiGenerationResponse)

```json
{
  "title": "Java Backend Developer",
  "description": "Разработка микросервисов на Java с использованием Spring Framework",
  "topics": ["Java Core", "Spring Framework", "Microservices", "Database Design"],
  "level": "middle",
  "questions": [
    {
      "text": "Расскажите о принципах SOLID",
      "type": "text",
      "order": 1
    }
  ]
}
```

## AI Промпт

Система использует структурированный промпт для получения предсказуемого JSON-ответа:

```
Сгенерируй структуру вакансии на основе следующих параметров:

Описание: {description}
Количество вопросов: {questionsCount}
Тип вопросов: {questionType} (варианты: hard, soft, mixed и т.д.)

Верни только JSON следующей структуры:
{
  "title": "string",
  "description": "string", 
  "topics": ["string", ...],
  "level": "junior|middle|senior|lead",
  "questions": [
    {
      "text": "string",
      "type": "text|audio|choice",
      "order": 1
    }
  ]
}
```

## Обработка ошибок

### Fallback логика

Если AI возвращает невалидный JSON или происходит ошибка:

1. **Парсинг JSON** - создается минимальный ответ с дефолтными значениями
2. **AI недоступен** - возвращается HTTP 500
3. **Некорректные входные данные** - возвращается HTTP 400

### Дефолтные значения

```java
// Минимальная вакансия при ошибке
title: "Новая вакансия"
description: "Описание вакансии"
level: "middle"
topics: []
questions: []
```

## Бизнес-логика

### Поля, устанавливаемые системой

Следующие поля НЕ генерируются AI, а устанавливаются системой:

- `status` → `ACTIVE`
- `language` → `"Русский"`
- `showOtherLang` → `true`
- `answerTime` → `150`
- `saveAudio` → `true`
- `saveVideo` → `true`
- `randomOrder` → `true`
- `questionType` → `"В основном хард-скиллы"`
- `questionsCount` → `5`
- `checkType` → `"Автоматическая проверка"`
- `minScore` → `6.0`

### Маппинг уровней

AI может вернуть уровень в любом формате, система маппит его в enum:

- `junior` → `JUNIOR`
- `middle` → `MIDDLE`
- `senior` → `SENIOR`
- `lead` → `LEAD`
- По умолчанию → `MIDDLE`

## Тестирование

### Примеры тестов

```java
@Test
void shouldGeneratePositionFromValidDescription() {
    // Given
    String description = "Java разработчик";
    
    // When
    PositionAiGenerationResponse response = aiController.generatePosition(request);
    
    // Then
    assertThat(response.getTitle()).isNotNull();
    assertThat(response.getLevel()).isNotNull();
    assertThat(response.getQuestions()).hasSize(5);
}

@Test
void shouldHandleInvalidJsonFromAI() {
    // Given
    String invalidJson = "invalid json";
    
    // When & Then
    // Должен вернуть дефолтную вакансию, а не упасть
}
```

## Мониторинг

### Логирование

- Уровень INFO: успешная генерация
- Уровень WARN: ошибки парсинга с fallback
- Уровень ERROR: критические ошибки AI

### Метрики

- Количество запросов к AI
- Время ответа AI
- Процент успешных генераций
- Статистика ошибок

## Безопасность

### Валидация входных данных

- `description` - обязательное поле, не пустое
- `questionsCount` - от 1 до 20
- `questionType` - валидные значения: hard, soft, mixed

### Ограничения

- Максимальная длина описания: 1000 символов
- Максимальное количество вопросов: 20
- Rate limiting: 10 запросов в минуту на пользователя

## Развертывание

### Конфигурация

```yaml
ai:
  openrouter:
    api-url: https://openrouter.ai/api/v1
    api-key: ${OPENROUTER_API_KEY}
    model: anthropic/claude-3.5-sonnet
    max-tokens: 2000
    temperature: 0.7
```

### Переменные окружения

- `OPENROUTER_API_KEY` - ключ API OpenRouter
- `AI_ENABLED` - включение/выключение AI функциональности

## Troubleshooting

### Частые проблемы

1. **AI не отвечает** - проверить API ключ и доступность сервиса
2. **Некорректный JSON** - проверить промпт и формат ответа
3. **Медленная генерация** - проверить настройки модели и токенов

### Диагностика

```bash
# Проверка доступности AI
curl -X POST /ai/generate-position \
  -H "Content-Type: application/json" \
  -d '{"description": "test"}'

# Просмотр логов
tail -f logs/application.log | grep "AI"
``` 