# 🚀 ТЗ: Расширение метода старта интервью для кандидатов

## 📋 Задача

Расширить метод `POST /interviews/{id}/start` для возврата всех необходимых данных кандидату при старте интервью, исключив избыточные запросы и чувствительные данные.

---

## 🎯 Цель

Заменить множественные API вызовы (`getInterview`, `getQuestions`, etc.) одним вызовом `startInterview`, который вернет все необходимые данные для проведения интервью кандидатом.

---

## 📊 Текущее состояние

### Проблемы:
1. **Избыточные запросы**: `getInterview()` возвращает чувствительные данные
2. **Дублирование**: данные кандидата уже есть в store
3. **Небезопасность**: кандидат видит данные компании, команды, других кандидатов
4. **Производительность**: множественные API вызовы

### Текущий flow:
```
1. authCandidate() → token + candidate data
2. getInterview() → interview + candidate + position + questions (ИЗБЫТОЧНО)
3. startInterview() → только статус
```

---

## 🔧 Требования к реализации

### 1. Расширенный запрос `InterviewStartRequest`

```yaml
InterviewStartRequest:
  type: object
  properties:
    voiceMode:
      type: boolean
      description: "Включить голосовой режим"
      default: false
    includeCandidateData:
      type: boolean
      description: "Включить данные для кандидата"
      default: true
    agentConfig:
      $ref: '#/components/schemas/AgentConfig'
    voiceSettings:
      $ref: '#/components/schemas/VoiceSettings'
    autoCreateAgent:
      type: boolean
      description: "Автоматически создать агента"
      default: true
```

### 2. Расширенный ответ `InterviewStartResponse`

```yaml
InterviewStartResponse:
  type: object
  properties:
    # Базовые данные интервью
    interviewId:
      type: integer
      format: int64
    status:
      $ref: '#/components/schemas/InterviewStartStatusEnum'
    message:
      type: string
    
    # Данные для голосового режима (если voiceMode=true)
    agentId:
      type: string
      description: "ID созданного агента"
    sessionId:
      type: string
      description: "ID голосовой сессии"
    webhookUrl:
      type: string
      description: "URL для webhook событий"
    
    # Данные для кандидата (если includeCandidateData=true)
    candidateData:
      $ref: '#/components/schemas/InterviewCandidateData'
```

### 3. Новая схема `InterviewCandidateData`

```yaml
InterviewCandidateData:
  type: object
  description: "Минимальные данные для кандидата"
  properties:
    # Базовые данные интервью
    interview:
      type: object
      properties:
        id:
          type: integer
          format: int64
        status:
          $ref: '#/components/schemas/InterviewStatusEnum'
        createdAt:
          type: string
          format: date-time
        startedAt:
          type: string
          format: date-time
        finishedAt:
          type: string
          format: date-time
      required: [id, status, createdAt]
    
    # Настройки интервью
    settings:
      type: object
      properties:
        answerTime:
          type: integer
          description: "Время на ответ в секундах"
          example: 120
        language:
          type: string
          description: "Язык интервью"
          example: "Русский"
        saveAudio:
          type: boolean
          description: "Сохранять аудио"
          example: true
        saveVideo:
          type: boolean
          description: "Сохранять видео"
          example: false
        randomOrder:
          type: boolean
          description: "Случайный порядок вопросов"
          example: false
        minScore:
          type: number
          format: float
          description: "Минимальный проходной балл"
          example: 70.0
      required: [answerTime, language, saveAudio, saveVideo, randomOrder, minScore]
    
    # Вопросы для интервью
    questions:
      type: array
      items:
        type: object
        properties:
          id:
            type: integer
            format: int64
          text:
            type: string
            description: "Текст вопроса"
          type:
            $ref: '#/components/schemas/QuestionTypeEnum'
          order:
            type: integer
            description: "Порядок вопроса"
          isRequired:
            type: boolean
            description: "Обязательный вопрос"
        required: [id, text, type, order]
      description: "Список вопросов для интервью"
    
    # Информация о вакансии (минимальная)
    position:
      type: object
      properties:
        title:
          type: string
          description: "Название вакансии"
          example: "Java Developer"
        level:
          $ref: '#/components/schemas/PositionLevelEnum'
          description: "Уровень позиции"
      required: [title, level]
    
    # Прогресс интервью
    progress:
      type: object
      properties:
        currentQuestion:
          type: integer
          description: "Номер текущего вопроса (начиная с 0)"
          example: 0
        totalQuestions:
          type: integer
          description: "Общее количество вопросов"
          example: 5
        answeredQuestions:
          type: integer
          description: "Количество отвеченных вопросов"
          example: 0
        remainingTime:
          type: integer
          description: "Оставшееся время в секундах"
          example: 600
      required: [currentQuestion, totalQuestions, answeredQuestions, remainingTime]
    
    # Чек-лист для подготовки
    checklist:
      type: array
      items:
        type: object
        properties:
          text:
            type: string
            description: "Текст пункта чек-листа"
          completed:
            type: boolean
            description: "Выполнен ли пункт"
        required: [text, completed]
      description: "Чек-лист для подготовки к интервью"
    
    # Информация для приглашения
    inviteInfo:
      type: object
      properties:
        language:
          type: string
          description: "Язык интервью"
          example: "Русский"
        questionsCount:
          type: integer
          description: "Количество вопросов"
          example: 5
        estimatedDuration:
          type: integer
          description: "Примерная длительность в минутах"
          example: 15
      required: [language, questionsCount, estimatedDuration]
  
  required: [interview, settings, questions, position, progress, checklist, inviteInfo]
```

---

## 🔄 Обновленный flow

### Новый flow:
```
1. authCandidate() → token + candidate data (в store)
2. startInterview(includeCandidateData=true) → все данные для интервью
3. Дальнейшие вызовы только для действий (answer, finish, etc.)
```

### Пример запроса:
```json
POST /interviews/123/start
{
  "voiceMode": false,
  "includeCandidateData": true,
  "autoCreateAgent": false
}
```

### Пример ответа:
```json
{
  "interviewId": 123,
  "status": "STARTED",
  "message": "Интервью успешно начато",
  "candidateData": {
    "interview": {
      "id": 123,
      "status": "in_progress",
      "createdAt": "2024-01-15T10:30:00Z",
      "startedAt": "2024-01-15T10:30:00Z"
    },
    "settings": {
      "answerTime": 120,
      "language": "Русский",
      "saveAudio": true,
      "saveVideo": false,
      "randomOrder": false,
      "minScore": 70.0
    },
    "questions": [
      {
        "id": 1,
        "text": "Расскажите о своем опыте работы с Java",
        "type": "text",
        "order": 1,
        "isRequired": true
      }
    ],
    "position": {
      "title": "Java Developer",
      "level": "middle"
    },
    "progress": {
      "currentQuestion": 0,
      "totalQuestions": 5,
      "answeredQuestions": 0,
      "remainingTime": 600
    },
    "checklist": [
      {
        "text": "Проверьте работу микрофона",
        "completed": false
      }
    ],
    "inviteInfo": {
      "language": "Русский",
      "questionsCount": 5,
      "estimatedDuration": 15
    }
  }
}
```

---

## 🛡️ Безопасность

### Что НЕ возвращается кандидату:
- ❌ Данные компании (`company`, `branding`)
- ❌ Информация о команде (`team`)
- ❌ Статистика (`stats`, `avgScore`)
- ❌ Список других кандидатов (`candidates`)
- ❌ Публичные ссылки (`publicLink`)
- ❌ AI оценки (`aiScore`)
- ❌ Ответы других кандидатов (`answers`)

### Что возвращается кандидату:
- ✅ Только его данные (уже есть в store)
- ✅ Настройки интервью
- ✅ Вопросы для интервью
- ✅ Прогресс и статус
- ✅ Минимальная информация о вакансии

---

## 🔧 Техническая реализация

### 1. Backend изменения

```java
@PostMapping("/{id}/start")
public ResponseEntity<InterviewStartResponse> startInterview(
    @PathVariable Long id,
    @RequestBody(required = false) InterviewStartRequest request) {
    
    Interview interview = interviewService.findById(id);
    
    // Запускаем интервью
    interviewService.startInterview(interview);
    
    InterviewStartResponse response = new InterviewStartResponse();
    response.setInterviewId(id);
    response.setStatus("STARTED");
    
    // Если запрошены данные для кандидата
    if (request != null && Boolean.TRUE.equals(request.getIncludeCandidateData())) {
        InterviewCandidateData candidateData = buildCandidateData(interview);
        response.setCandidateData(candidateData);
    }
    
    // Если запрошен голосовой режим
    if (request != null && Boolean.TRUE.equals(request.getVoiceMode())) {
        Agent agent = agentService.createAgentForInterview(interview);
        response.setAgentId(agent.getElevenLabsAgentId());
        response.setSessionId(agent.getSessionId());
        response.setWebhookUrl(buildWebhookUrl(interview.getId()));
    }
    
    return ResponseEntity.ok(response);
}

private InterviewCandidateData buildCandidateData(Interview interview) {
    InterviewCandidateData data = new InterviewCandidateData();
    
    // Базовые данные интервью
    data.setInterview(buildInterviewData(interview));
    
    // Настройки из позиции
    Position position = positionService.findById(interview.getPositionId());
    data.setSettings(buildSettings(position));
    
    // Вопросы
    List<Question> questions = questionService.findByPositionId(position.getId());
    data.setQuestions(buildQuestionsData(questions));
    
    // Информация о позиции
    data.setPosition(buildPositionData(position));
    
    // Прогресс
    data.setProgress(buildProgress(interview, questions.size()));
    
    // Чек-лист
    data.setChecklist(buildChecklist());
    
    // Информация для приглашения
    data.setInviteInfo(buildInviteInfo(position, questions.size()));
    
    return data;
}
```

### 2. Frontend изменения

```typescript
// В InterviewSession.tsx и ElabsSession.tsx
const initializeSession = async () => {
  try {
    // Получаем все данные одним запросом
    const startResponse = await candidateApiService.startInterview(interviewId, {
      includeCandidateData: true,
      voiceMode: false // или true для ElabsSession
    });
    
    const { candidateData } = startResponse;
    
    // Устанавливаем данные из ответа
    setInterview(candidateData.interview);
    setInterviewSettings(candidateData.settings);
    setQuestions(candidateData.questions);
    setPosition(candidateData.position);
    setProgress(candidateData.progress);
    setChecklist(candidateData.checklist);
    setInviteInfo(candidateData.inviteInfo);
    
    // Данные кандидата берем из store
    const { user: candidate } = useAuthStore();
    setCandidate(candidate);
    
  } catch (error) {
    setError('Ошибка инициализации сессии');
  }
};
```

---

## 📋 План реализации

### Этап 1: Backend (2-3 дня)
1. ✅ Создать схему `InterviewCandidateData`
2. ✅ Расширить `InterviewStartRequest` и `InterviewStartResponse`
3. ✅ Реализовать метод `buildCandidateData()`
4. ✅ Добавить валидацию и обработку ошибок
5. ✅ Написать тесты

### Этап 2: Frontend (1-2 дня)
1. ✅ Обновить `candidateApiService.startInterview()`
2. ✅ Изменить `InterviewSession.tsx`
3. ✅ Изменить `ElabsSession.tsx`
4. ✅ Убрать вызовы `getInterview()`
5. ✅ Обновить типы данных

### Этап 3: Тестирование (1 день)
1. ✅ Протестировать новый flow
2. ✅ Проверить безопасность данных
3. ✅ Убедиться в совместимости с админским API
4. ✅ Проверить производительность

---

## 🎯 Результат

После реализации:
- 🔒 **Безопасность**: Кандидаты не видят чувствительные данные
- ⚡ **Производительность**: Один запрос вместо множественных
- 🧹 **Чистота кода**: Четкое разделение данных
- 🛡️ **Принцип минимальных прав**: Только необходимые данные
- 📊 **Удобство**: Все данные для UI в одном ответе

---

**Статус**: 🚀 **ГОТОВО К РЕАЛИЗАЦИИ**

**Приоритет**: 🔥 **ВЫСОКИЙ** - критически важно для безопасности 