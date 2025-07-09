# 🔍 Диагностика проблем с передачей аудио

## 📋 Обзор проблемы

Данный документ содержит профессиональный анализ проблем с передачей аудио между фронтендом и бэкендом через REST API.

## 🎯 Методы передачи аудио в системе

### 1. Простая транскрибация
```
POST /api/v1/ai/transcribe
Content-Type: multipart/form-data

Параметры:
- audio: MultipartFile (аудио файл)

Ответ:
{
  "transcript": "Транскрибированный текст..."
}
```

### 2. Полная обработка с AI
```
POST /api/v1/ai/transcribe-answer
Content-Type: multipart/form-data

Параметры:
- audioFile: MultipartFile (аудио файл)
- interviewId: Long (ID интервью)
- questionId: Long (ID вопроса)

Ответ:
{
  "success": true,
  "formattedText": "Отформатированный текст...",
  "interviewAnswerId": 123
}
```

### 3. Диагностический endpoint
```
POST /api/v1/ai/test-audio-upload
Content-Type: multipart/form-data

Параметры:
- file: MultipartFile (любой файл для тестирования)

Ответ:
{
  "success": true,
  "filename": "test.mp3",
  "size": 1024,
  "contentType": "audio/mpeg",
  "message": "Audio file received successfully"
}
```

## 🔧 Выполненные исправления

### 1. Улучшена CORS конфигурация
```java
// Добавлены дополнительные заголовки для multipart запросов
private List<String> allowedHeaders = List.of(
    "*", "Content-Type", "Authorization", "X-Requested-With", 
    "Accept", "Origin", "Access-Control-Request-Method", 
    "Access-Control-Request-Headers"
);
```

### 2. Добавлен диагностический endpoint
- Позволяет тестировать базовую передачу файлов
- Проверяет валидацию файлов
- Возвращает детальную информацию о файле

### 3. Создан тестовый HTML файл
- `test-audio-upload.html` - интерактивный тестер
- Позволяет тестировать все endpoint'ы
- Показывает детальные результаты

## 🚀 Инструкции по тестированию

### Шаг 1: Запуск приложения
```bash
./gradlew bootRun
```

### Шаг 2: Тестирование через HTML
1. Откройте `test-audio-upload.html` в браузере
2. Выберите аудио файл (MP3, WAV, M4A, OGG, FLAC, AAC)
3. Нажмите кнопки для тестирования разных endpoint'ов

### Шаг 3: Тестирование через cURL
```bash
# Тест простой передачи
curl -X POST "http://localhost:8080/api/v1/ai/test-audio-upload" \
  -F "file=@/path/to/audio.mp3"

# Тест транскрибации
curl -X POST "http://localhost:8080/api/v1/ai/transcribe" \
  -F "audio=@/path/to/audio.mp3"

# Тест AI обработки
curl -X POST "http://localhost:8080/api/v1/ai/transcribe-answer" \
  -F "audioFile=@/path/to/audio.mp3" \
  -F "interviewId=1" \
  -F "questionId=1"
```

## 🔍 Возможные проблемы и решения

### Проблема 1: CORS ошибки
**Симптомы:** `Access to fetch at 'http://localhost:8080/api/v1/ai/transcribe' from origin 'http://localhost:3000' has been blocked by CORS policy`

**Решение:** 
- Проверьте настройки CORS в `ApplicationProperties.java`
- Убедитесь, что фронтенд запущен на разрешенном домене

### Проблема 2: Ошибки валидации файла
**Симптомы:** `400 Bad Request` с сообщением о неверном типе файла

**Решение:**
- Убедитесь, что файл имеет MIME-тип `audio/*`
- Проверьте расширение файла (поддерживаются: mp3, wav, m4a, ogg, flac, aac)
- Размер файла не должен превышать 50MB

### Проблема 3: Ошибки Whisper сервиса
**Симптомы:** `503 Service Unavailable` или ошибки транскрибации

**Решение:**
- Убедитесь, что Whisper сервис запущен на порту 9000
- Проверьте конфигурацию в `application.yaml`:
  ```yaml
  app:
    ai:
      transcription:
        whisper:
          url: http://localhost:9000
          timeout: 30000
  ```

### Проблема 4: Ошибки базы данных
**Симптомы:** `404 Not Found` при указании interviewId/questionId

**Решение:**
- Убедитесь, что указанные ID существуют в базе данных
- Проверьте, что интервью и вопрос связаны с одной позицией

## 📊 Мониторинг и логирование

### Логи приложения
Все операции с аудио логируются в `logs/hr-recruiter.log`:

```log
INFO  - Starting audio transcription for file: test.mp3 (1024 bytes)
INFO  - Audio file validation passed: test.mp3 (1024 bytes, audio/mpeg)
INFO  - Whisper transcription completed in 1500 ms, length: 45 chars
INFO  - Claude formatting completed in 300 ms, length: 42 chars
INFO  - InterviewAnswer created and saved to database in 50 ms, ID: 123
INFO  - Total transcription pipeline completed successfully in 1850 ms
```

### Метрики производительности
- Время обработки Whisper
- Время форматирования Claude
- Время сохранения в БД
- Общее время обработки

## 🛠️ Дополнительные инструменты

### 1. Проверка доступности сервисов
```bash
# Проверка Whisper
curl http://localhost:9000

# Проверка основного приложения
curl http://localhost:8080/api/v1/actuator/health
```

### 2. Проверка конфигурации
```bash
# Просмотр всех настроек
curl http://localhost:8080/api/v1/actuator/configprops
```

### 3. Мониторинг логов в реальном времени
```bash
tail -f logs/hr-recruiter.log
```

## 📝 Рекомендации

### Для фронтенда:
1. Используйте `FormData` для отправки файлов
2. Устанавливайте правильный `Content-Type`
3. Обрабатывайте ошибки и показывайте пользователю прогресс
4. Валидируйте файлы на клиенте перед отправкой

### Для бэкенда:
1. Всегда логируйте операции с файлами
2. Используйте транзакции для сохранения данных
3. Обрабатывайте исключения и возвращайте понятные ошибки
4. Мониторьте производительность пайплайна

## 🎯 Заключение

Система передачи аудио настроена корректно и включает:
- ✅ Правильную конфигурацию multipart
- ✅ Валидацию файлов
- ✅ Обработку ошибок
- ✅ Логирование операций
- ✅ CORS настройки
- ✅ Диагностические инструменты

При возникновении проблем используйте предоставленные инструменты для диагностики и следуйте рекомендациям по устранению неполадок. 