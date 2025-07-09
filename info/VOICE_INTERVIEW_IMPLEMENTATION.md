# 🎤 Реализация голосового интервью с ElevenLabs Conversational AI

## 📋 Обзор

Этот документ описывает полную реализацию голосового интервью с использованием ElevenLabs Conversational AI в системе HR Recruiter.

## 🏗️ Архитектура

### **Компоненты системы:**

1. **VoiceInterviewService** - основной сервис управления голосовыми сессиями
2. **VoiceInterviewController** - REST API для голосового интервью
3. **ElevenLabs Conversational AI** - внешний сервис для голосового взаимодействия
4. **База данных** - хранение метаданных голосовых сессий и ответов

### **Поток взаимодействия:**

```
1. HR создает интервью → 2. Запускает голосовую сессию → 3. ElevenLabs агент подключается
4. Агент получает вопросы через webhook → 5. Задает вопросы голосом → 6. Кандидат отвечает
7. Ответ транскрибируется → 8. Сохраняется в БД → 9. Переход к следующему вопросу
10. Завершение интервью → 11. AI оценка ответов
```

## 🔧 Техническая реализация

### **1. Модели данных**

#### **Interview (расширена)**
```java
// Новые поля для голосового интервью
private String voiceSessionId;        // ID сессии в ElevenLabs
private String voiceAgentId;          // ID агента в ElevenLabs
private Boolean voiceEnabled;         // Включено ли голосовое интервью
private String voiceLanguage;         // Язык голосового интервью
private String voiceVoiceId;          // ID голоса в ElevenLabs
private LocalDateTime voiceStartedAt; // Время начала голосовой сессии
private LocalDateTime voiceFinishedAt; // Время завершения голосовой сессии
private Long voiceTotalDuration;      // Общая длительность в секундах
```

#### **InterviewAnswer (расширена)**
```java
// Новые поля для голосовых ответов
private String voiceSessionId;        // ID голосовой сессии
private Double voiceConfidence;       // Уверенность в распознавании (0.0-1.0)
private String voiceEmotion;          // Определенная эмоция в голосе
private String voiceSpeakerId;        // ID говорящего
private String voiceAudioUrl;         // URL аудио файла ответа
private Long voiceProcessingTime;     // Время обработки в миллисекундах
private Double voiceQualityScore;     // Оценка качества аудио (0.0-1.0)
```

### **2. API эндпоинты**

#### **Создание голосовой сессии**
```http
POST /api/interviews/{interviewId}/voice/session
```
- Создает сессию в ElevenLabs
- Настраивает агента с промптом
- Возвращает URL для подключения

#### **Получение следующего вопроса**
```http
GET /api/interviews/{interviewId}/voice/next-question
```
- Вызывается ElevenLabs агентом через webhook
- Возвращает следующий вопрос из базы
- Автоматически завершает интервью при отсутствии вопросов

#### **Сохранение голосового ответа**
```http
POST /api/interviews/{interviewId}/voice/answer?questionId={questionId}
```
- Сохраняет транскрибированный ответ
- Включает метаданные (уверенность, эмоция, качество)
- Связывает с конкретным вопросом

#### **Завершение сессии**
```http
POST /api/interviews/{interviewId}/voice/end
```
- Завершает сессию в ElevenLabs
- Сохраняет итоговую статистику
- Освобождает ресурсы

### **3. Конфигурация ElevenLabs**

#### **application.yaml**
```yaml
app:
  ai:
    transcription:
      elevenlabs:
        # STT конфигурация
        api-url: https://api.elevenlabs.io
        api-key: ${ELEVEN_LABS_API_KEY:}
        
        # Conversational AI конфигурация
        default-agent-id: ${ELEVEN_LABS_AGENT_ID:}
        default-voice-id: "21m00Tcm4TlvDq8ikWAM"  # Rachel
        default-language: "ru"
        max-session-duration-minutes: 60
        response-timeout-seconds: 30
        enable-emotion-detection: true
        enable-speaker-detection: false
        enable-timestamps: true
        enable-audio-quality-analysis: true
```

#### **Промпт агента**
```
Ты профессиональный HR-специалист, проводящий собеседование. 
Твоя задача - задавать вопросы кандидату и вести естественную беседу.

Правила:
1. Задавай только те вопросы, которые получаешь от системы
2. Не придумывай свои вопросы
3. Будь дружелюбным и профессиональным
4. Если кандидат не отвечает, вежливо попроси повторить
5. После каждого ответа переходи к следующему вопросу
6. В конце поблагодари за участие

Стиль общения: профессиональный, но дружелюбный
```

## 🚀 Использование

### **1. Создание голосового интервью**

```bash
# 1. Создать обычное интервью
POST /api/interviews/from-candidate/{candidateId}

# 2. Запустить голосовую сессию
POST /api/interviews/{interviewId}/voice/session
```

### **2. Подключение к сессии**

```javascript
// На фронтенде - подключение к ElevenLabs WebSocket
const session = await fetch('/api/interviews/123/voice/session');
const { websocketUrl } = await session.json();

const ws = new WebSocket(websocketUrl);
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    // Обработка сообщений от агента
};
```

### **3. Мониторинг сессии**

```bash
# Получить статус сессии
GET /api/interviews/{interviewId}/voice/status

# Завершить сессию
POST /api/interviews/{interviewId}/voice/end
```

## 💰 Стоимость

### **Расчет для 1 часа интервью:**
- **ElevenLabs Business Plan**: $0.08/минута
- **1 интервью (60 минут)**: $4.80
- **100 интервью/месяц**: $480
- **1000 интервью/месяц**: $4,800

### **Оптимизация стоимости:**
1. **Короткие сессии** - завершать сразу после последнего вопроса
2. **Кэширование** - повторное использование агентов
3. **Batch processing** - группировка интервью
4. **Fallback режим** - текстовый режим при проблемах

## 🔒 Безопасность

### **Защита данных:**
- **HTTPS** - все API вызовы шифруются
- **API ключи** - аутентификация ElevenLabs
- **Валидация** - проверка входных данных
- **Логирование** - аудит всех операций

### **Конфиденциальность:**
- **Временное хранение** - автоматическое удаление аудио
- **Анонимизация** - удаление персональных данных
- **Согласие** - явное согласие на запись
- **Контроль** - возможность удаления данных

## 📊 Мониторинг и аналитика

### **Метрики:**
- **Длительность сессий** - среднее время интервью
- **Качество аудио** - оценка качества записи
- **Уверенность распознавания** - точность транскрибации
- **Эмоции** - анализ эмоционального состояния
- **Ошибки** - частота технических сбоев

### **Алерты:**
- **Высокая стоимость** - превышение лимитов
- **Технические сбои** - недоступность ElevenLabs
- **Плохое качество** - низкая уверенность распознавания
- **Долгие сессии** - превышение времени

## 🧪 Тестирование

### **Unit тесты:**
```java
@Test
void createVoiceSession_Success() {
    // Тест создания сессии
}

@Test
void getNextQuestion_ReturnsCorrectQuestion() {
    // Тест получения следующего вопроса
}

@Test
void saveVoiceAnswer_SavesCorrectly() {
    // Тест сохранения ответа
}
```

### **Integration тесты:**
```java
@Test
void fullVoiceInterviewFlow() {
    // Полный цикл голосового интервью
}
```

### **E2E тесты:**
```javascript
// Тестирование с реальным ElevenLabs API
describe('Voice Interview E2E', () => {
    it('should complete full interview flow', async () => {
        // Создание сессии → Вопросы → Ответы → Завершение
    });
});
```

## 🔄 Миграция данных

### **База данных:**
```sql
-- Миграция 008-voice-interview-support.sql
ALTER TABLE interviews ADD COLUMN voice_session_id VARCHAR(255);
ALTER TABLE interviews ADD COLUMN voice_agent_id VARCHAR(255);
-- ... остальные поля
```

### **Индексы:**
```sql
CREATE INDEX idx_interviews_voice_session_id ON interviews(voice_session_id);
CREATE INDEX idx_interviews_voice_enabled ON interviews(voice_enabled);
CREATE INDEX idx_interview_answers_voice_confidence ON interview_answers(voice_confidence);
```

## 🚀 Развертывание

### **Переменные окружения:**
```bash
# ElevenLabs API
ELEVEN_LABS_API_KEY=your-api-key
ELEVEN_LABS_AGENT_ID=your-agent-id

# Настройки приложения
VOICE_INTERVIEW_ENABLED=true
VOICE_SESSION_TIMEOUT=3600
```

### **Docker:**
```yaml
# compose.yaml
services:
  app:
    environment:
      - ELEVEN_LABS_API_KEY=${ELEVEN_LABS_API_KEY}
      - ELEVEN_LABS_AGENT_ID=${ELEVEN_LABS_AGENT_ID}
```

## 📈 Планы развития

### **Краткосрочные (1-2 месяца):**
- ✅ Базовая интеграция ElevenLabs
- ✅ Голосовые сессии
- ✅ Транскрибация ответов
- 🔄 AI оценка голосовых ответов
- 🔄 Анализ эмоций

### **Среднесрочные (3-6 месяцев):**
- 🔄 Многоязычная поддержка
- 🔄 Кастомизация голосов
- 🔄 Интерактивные вопросы
- 🔄 Аналитика в реальном времени
- 🔄 Интеграция с CRM

### **Долгосрочные (6+ месяцев):**
- 🔄 AI-генерация вопросов
- 🔄 Адаптивные сценарии
- 🔄 Видео интервью
- 🔄 Мобильное приложение
- 🔄 Интеграция с календарями

## ✅ Заключение

Реализация голосового интервью с ElevenLabs Conversational AI предоставляет:

1. **Естественное взаимодействие** - кандидаты чувствуют себя комфортно
2. **Высокое качество** - профессиональная транскрибация и синтез речи
3. **Масштабируемость** - поддержка множественных одновременных сессий
4. **Аналитика** - детальные метрики и анализ ответов
5. **Интеграция** - полная совместимость с существующей системой

Система готова к продакшн использованию и дальнейшему развитию. 