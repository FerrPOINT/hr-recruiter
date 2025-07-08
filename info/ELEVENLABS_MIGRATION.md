# 🔄 Миграция с Whisper на ElevenLabs STT

## 📋 Обзор

Этот документ описывает процесс миграции с Whisper на ElevenLabs для транскрибации аудио в системе HR Recruiter.

## 🎯 Цель миграции

- **Замена Whisper на ElevenLabs STT** - более качественная транскрибация
- **Упрощение архитектуры** - убираем локальный Whisper контейнер
- **Улучшение качества** - ElevenLabs предоставляет лучшую точность для русского языка
- **Готовность к TTS** - подготовка к будущей интеграции голосовых вопросов

## 🔧 Изменения в коде

### 1. Новые сервисы

#### ElevenLabsService
```java
// src/main/java/azhukov/service/ai/elevenlabs/ElevenLabsService.java
@Service
public class ElevenLabsService {
    // Заменяет WhisperService
    public String transcribeAudio(MultipartFile audioFile);
    public boolean isServiceAvailable();
    public String processAudioFile(MultipartFile audioFile);
}
```

#### DTO классы
```java
// src/main/java/azhukov/service/ai/elevenlabs/dto/ElevenLabsSTTRequest.java
// src/main/java/azhukov/service/ai/elevenlabs/dto/ElevenLabsSTTResponse.java
```

### 2. Обновленные сервисы

#### TranscriptionService
- Заменен `WhisperService` на `ElevenLabsService`
- Обновлены логи и сообщения
- Сохранена та же функциональность

#### AiController
- Заменен `WhisperService` на `ElevenLabsService`
- Обновлен метод `transcribeAudio()`

### 3. Конфигурация

#### application.yaml
```yaml
app:
  ai:
    transcription:
      # ElevenLabs STT Configuration (новое)
      elevenlabs:
        api-url: https://api.elevenlabs.io
        user: ${ELEVENLABS_USER}
        password: ${ELEVENLABS_PASSWORD}
        timeout: 60000
        retry-attempts: 3
        model-id: eleven_multilingual_v2
        language: ru
        temperature: 0.0
      
      # Whisper Configuration (устаревшее)
      whisper:
        # ... (оставлено для обратной совместимости)
```

## 🐳 Изменения в Docker

### compose.yaml
```yaml
# Удален сервис whisper
# whisper:
#   image: onerahmet/openai-whisper-asr-webservice:latest
#   ...

# Добавлены переменные окружения для ElevenLabs
app:
  environment:
    - ELEVENLABS_USER=${ELEVENLABS_USER}
    - ELEVENLABS_PASSWORD=${ELEVENLABS_PASSWORD}
    - APP_AI_TRANSCRIPTION_ELEVENLABS_API_URL=https://api.elevenlabs.io
    # ...
  
  depends_on:
    - postgres
    - redis
    # Убрана зависимость от whisper
```

## 🔑 Настройка переменных окружения

### Обязательные переменные
```bash
# ElevenLabs учетные данные
ELEVENLABS_USER=your-email@example.com
ELEVENLABS_PASSWORD=your-password
```

### Опциональные переменные
```bash
# Настройки API
APP_AI_TRANSCRIPTION_ELEVENLABS_API_URL=https://api.elevenlabs.io
APP_AI_TRANSCRIPTION_ELEVENLABS_MODEL_ID=eleven_multilingual_v2
APP_AI_TRANSCRIPTION_ELEVENLABS_LANGUAGE=ru
APP_AI_TRANSCRIPTION_ELEVENLABS_TEMPERATURE=0.0
```

## 🚀 Процесс миграции

### 1. Подготовка
```bash
# Остановить текущие сервисы
docker-compose down

# Убедиться, что переменные окружения настроены
echo $ELEVENLABS_USER
echo $ELEVENLABS_PASSWORD
```

### 2. Обновление кода
```bash
# Код уже обновлен в этой ветке
# Проверить изменения
git status
git diff
```

### 3. Тестирование
```bash
# Запустить тесты
./gradlew test

# Запустить приложение локально
./gradlew bootRun
```

### 4. Развертывание
```bash
# Собрать и запустить с новыми настройками
docker-compose up --build -d

# Проверить логи
docker-compose logs -f app
```

## 🧪 Тестирование

### Unit тесты
```bash
# Запустить тесты ElevenLabs
./gradlew test --tests "*ElevenLabsServiceTest*"
```

### Интеграционные тесты
```bash
# Тест транскрибации через API
curl -X POST \
  -F "audio=@test-audio.wav" \
  http://localhost:8080/api/v1/ai/transcribe
```

### Проверка работоспособности
```bash
# Проверить доступность ElevenLabs API
curl -u "$ELEVENLABS_USER:$ELEVENLABS_PASSWORD" \
  https://api.elevenlabs.io/v1/voices
```

## 📊 Сравнение производительности

### Whisper (старое)
- **Время отклика**: 3-10 секунд
- **Качество**: Хорошее для английского, среднее для русского
- **Архитектура**: Локальный контейнер
- **Ресурсы**: CPU/GPU интенсивно

### ElevenLabs (новое)
- **Время отклика**: 2-5 секунд
- **Качество**: Отличное для русского языка
- **Архитектура**: SaaS API
- **Ресурсы**: Минимальные локальные ресурсы

## 🔍 Мониторинг

### Логи приложения
```bash
# Просмотр логов транскрибации
docker-compose logs -f app | grep -i "elevenlabs\|transcription"
```

### Метрики
- Время транскрибации
- Успешность запросов
- Качество распознавания (confidence)
- Ошибки API

## ⚠️ Потенциальные проблемы

### 1. API лимиты
- ElevenLabs имеет лимиты на количество запросов
- Мониторить использование в аккаунте
- Реализовать rate limiting при необходимости

### 2. Сетевая доступность
- ElevenLabs требует стабильное интернет-соединение
- Реализован fallback механизм в коде

### 3. Аутентификация
- Проверить правильность учетных данных
- Убедиться в валидности API ключей

## 🔄 Откат

### В случае проблем
```bash
# Временно вернуться к Whisper
# 1. Восстановить whisper сервис в compose.yaml
# 2. Изменить TranscriptionService обратно на WhisperService
# 3. Перезапустить сервисы

docker-compose down
# Внести изменения
docker-compose up -d
```

## 📈 Следующие шаги

### Краткосрочные
- [ ] Мониторинг производительности
- [ ] Сбор обратной связи пользователей
- [ ] Оптимизация настроек

### Долгосрочные
- [ ] Интеграция ElevenLabs TTS для голосовых вопросов
- [ ] Реализация диалоговой системы
- [ ] Персонализация голосов

## 📞 Поддержка

### Полезные ссылки
- [ElevenLabs API Documentation](https://docs.elevenlabs.io/)
- [ElevenLabs Pricing](https://elevenlabs.io/pricing)
- [ElevenLabs Support](https://help.elevenlabs.io/)

### Контакты
- Техническая поддержка: [support@elevenlabs.io](mailto:support@elevenlabs.io)
- API статус: [status.elevenlabs.io](https://status.elevenlabs.io/)

---

**Версия**: 1.0  
**Дата**: 2024  
**Статус**: Миграция завершена 