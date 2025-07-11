# 🔧 ТЗ: Исправление критических и средних проблем HR-Recruiter Backend

## 📋 Общая информация

**Проект**: HR-Recruiter Backend  
**Приоритет**: 🔥 КРИТИЧЕСКИЙ  
**Срок выполнения**: 1-2 недели  
**Статус**: Требует немедленного исправления  

---

## 🎯 Цель

Исправить критические проблемы безопасности и средние проблемы функциональности для обеспечения стабильной и безопасной работы системы.

---

## 🔥 КРИТИЧЕСКИЕ ПРОБЛЕМЫ (ИСПРАВИТЬ НЕМЕДЛЕННО)

### 1. JWT Secret в конфигурации

**Файл**: `src/main/resources/application.yaml`  
**Строка**: 130  
**Проблема**: Хардкод JWT секрета в конфигурации

#### Текущий код:
```yaml
security:
  jwt:
    secret: a3f4d4bf5c8d4be28c195fe343d5b68161503ecf103e42779cfe5e0f0d86c3a3
```

#### Требуемые изменения:
```yaml
security:
  jwt:
    secret: ${JWT_SECRET:}
```

#### Дополнительные действия:
1. Создать файл `.env` в корне проекта
2. Добавить в `.env`:
   ```
   JWT_SECRET=your-secure-random-secret-here
   ```
3. Добавить `.env` в `.gitignore`
4. Создать `.env.example` с примером:
   ```
   JWT_SECRET=your-jwt-secret-here
   ```

### 2. API ключи в конфигурации

**Файлы**: 
- `src/main/resources/application.yaml` (строка 67)
- `src/test/resources/application.yaml` (строки 21-23)

#### Проблемы:
1. Тестовые API ключи в продакшн конфигурации
2. Опечатка в переменной `OPEAN_AI_API_KEY` (должно быть `OPENAI_API_KEY`)

#### Требуемые изменения:

**В `src/main/resources/application.yaml`:**
```yaml
# Было:
api-key: ${OPEAN_AI_API_KEY:sk-test-openai-key}

# Должно быть:
api-key: ${OPENAI_API_KEY:}
```

**В `src/test/resources/application.yaml`:**
```yaml
# Было:
api-key: ${OPEAN_AI_API_KEY:sk-test-openai-key}

# Должно быть:
api-key: ${OPENAI_API_KEY:sk-test-key}
```

#### Дополнительные действия:
1. Обновить все упоминания `OPEAN_AI_API_KEY` на `OPENAI_API_KEY`
2. Добавить в `.env`:
   ```
   OPENAI_API_KEY=your-openai-api-key
   ELEVEN_LABS_API_KEY=your-elevenlabs-key
   OPENROUTER_API_KEY=your-openrouter-key
   ```

### 3. CORS настройки

**Файл**: `src/main/resources/application.yaml`  
**Строки**: 150-155

#### Текущий код:
```yaml
cors:
  allowed-origins: "*"
  allowed-methods: "*"
  allowed-headers: "*"
  allow-credentials: true
```

#### Требуемые изменения:
```yaml
cors:
  allowed-origins: ${ALLOWED_ORIGINS:http://localhost:3000}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: Authorization,Content-Type,X-Requested-With,Accept,Origin
  allow-credentials: false
  max-age: 3600
```

#### Дополнительные действия:
1. Добавить в `.env`:
   ```
   ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com
   ```

---

## ⚠️ СРЕДНИЕ ПРОБЛЕМЫ (ИСПРАВИТЬ В ТЕЧЕНИЕ НЕДЕЛИ)

### 4. Недостаточная валидация в контроллерах

**Проблема**: Только 1 из 15 контроллеров использует `@Valid`

#### Список контроллеров для исправления:

1. **InterviewsApiController.java**
   ```java
   // Добавить @Valid во все методы с @RequestBody
   @PostMapping("/{id}/start")
   public ResponseEntity<InterviewStartResponse> startInterview(
       @PathVariable Long id,
       @Valid @RequestBody(required = false) InterviewStartRequest request) {
   ```

2. **PositionsApiController.java**
   ```java
   @PostMapping
   public ResponseEntity<Position> createPosition(
       @Valid @RequestBody PositionCreateRequest request) {
   
   @PutMapping("/{id}")
   public ResponseEntity<Position> updatePosition(
       @PathVariable Long id,
       @Valid @RequestBody PositionUpdateRequest request) {
   ```

3. **QuestionsApiController.java**
   ```java
   @PostMapping
   public ResponseEntity<Question> createQuestion(
       @Valid @RequestBody QuestionCreateRequest request) {
   
   @PutMapping("/{id}")
   public ResponseEntity<Question> updateQuestion(
       @PathVariable Long id,
       @Valid @RequestBody QuestionUpdateRequest request) {
   ```

4. **UsersApiController.java**
   ```java
   @PostMapping
   public ResponseEntity<User> createUser(
       @Valid @RequestBody UserCreateRequest request) {
   
   @PutMapping("/{id}")
   public ResponseEntity<User> updateUser(
       @PathVariable Long id,
       @Valid @RequestBody UserUpdateRequest request) {
   ```

5. **SettingsApiController.java**
   ```java
   @PutMapping("/branding")
   public ResponseEntity<Branding> updateBranding(
       @Valid @RequestBody BrandingUpdateRequest request) {
   ```

6. **AiController.java**
   ```java
   @PostMapping("/generate-position")
   public ResponseEntity<PositionAiGenerationResponse> generatePosition(
       @Valid @RequestBody PositionAiGenerationRequest request) {
   
   @PostMapping("/generate-position-data")
   public ResponseEntity<PositionDataGenerationResponse> generatePositionData(
       @Valid @RequestBody PositionDataGenerationRequest request) {
   ```

7. **AgentsApiController.java**
   ```java
   @PostMapping
   public ResponseEntity<Agent> createAgent(
       @Valid @RequestBody AgentCreateRequest request) {
   
   @PutMapping("/{id}")
   public ResponseEntity<Agent> updateAgent(
       @PathVariable Long id,
       @Valid @RequestBody AgentUpdateRequest request) {
   ```

8. **VoiceInterviewController.java**
   ```java
   @PostMapping("/{interviewId}/voice/session")
   public ResponseEntity<VoiceSessionResponse> createVoiceSession(
       @PathVariable Long interviewId,
       @Valid @RequestBody(required = false) VoiceSessionCreateRequest request) {
   ```

9. **WebhookController.java**
   ```java
   @PostMapping("/elevenlabs/events")
   public ResponseEntity<?> handleElevenLabsWebhook(
       @Valid @RequestBody ElevenLabsWebhookEvent event) {
   ```

10. **AuthController.java**
    ```java
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request) {
    ```

11. **AccountController.java**
    ```java
    @PutMapping
    public ResponseEntity<User> updateAccount(
        @Valid @RequestBody UserUpdateRequest request) {
    ```

12. **DefaultController.java**
    ```java
    // Добавить валидацию если есть методы с @RequestBody
    ```

### 5. Логирование в тестах

**Проблема**: 50+ `System.out.println` в тестовых классах

#### Файлы для исправления:

1. **VoiceInterviewIntegrationTest.java**
   ```java
   // Добавить в начало класса:
   @Slf4j
   
   // Заменить все System.out.println на:
   log.info("✅ VoiceInterviewService bean создан успешно");
   log.warn("⚠️ WebhookService bean не найден");
   log.debug("🔧 ElevenLabs конфигурация валидна: {}", isValid);
   ```

2. **OpenRouterLocalTest.java**
   ```java
   @Slf4j
   
   // Заменить:
   log.warn("WARNING: OPENROUTER_API_KEY не установлен. Тест будет пропущен.");
   log.info("OpenRouter API KEY: {}", config.getApiKey());
   log.debug("Результат: {}", result);
   ```

3. **ControllerValidationTest.java**
   ```java
   @Slf4j
   
   // Заменить:
   log.info("🔍 Проверка контроллеров...");
   log.info("📊 Статистика: {} API интерфейсов, {} контроллеров", apiInterfacesCount, controllersCount);
   ```

4. **AuthSeparationIntegrationTest.java**
   ```java
   @Slf4j
   
   // Заменить:
   log.debug("ADMIN LOGIN RESPONSE: status={}, body={}", status, response);
   log.debug("CANDIDATE AUTH RESPONSE: status={}, body={}", status, response);
   ```

### 6. Незавершенные TODO в критических сервисах

#### AgentService.java

**Строка 53**: Создать агента в ElevenLabs
```java
// TODO: Создать агента в ElevenLabs
// Реализовать:
public Agent createAgentInElevenLabs(AgentCreateRequest request) {
    // 1. Вызвать ElevenLabs API для создания агента
    // 2. Сохранить ID агента в базе данных
    // 3. Вернуть созданного агента
}
```

**Строка 121**: Удалить агента из ElevenLabs
```java
// TODO: Удалить агента из ElevenLabs
// Реализовать:
public void deleteAgentFromElevenLabs(String elevenLabsAgentId) {
    // 1. Вызвать ElevenLabs API для удаления агента
    // 2. Обновить статус в базе данных
}
```

**Строка 140**: Реальное время ответа
```java
// TODO: Реальное время ответа
// Заменить:
response.setDuration(1000L);
// На:
response.setDuration(calculateRealResponseTime());
```

#### WebhookService.java

**Строка 98**: Сохранить в базу данных
```java
// TODO: Сохранить в базу данных
// Реализовать:
private void saveWebhookEvent(ElevenLabsWebhookEvent event) {
    // 1. Создать запись в activity_log
    // 2. Сохранить метаданные события
}
```

**Строка 180**: Запустить анализ результатов интервью
```java
// TODO: Запустить анализ результатов интервью
// Реализовать:
private void analyzeInterviewResults(Long interviewId) {
    // 1. Получить все ответы интервью
    // 2. Запустить AI анализ
    // 3. Обновить результаты интервью
}
```

**Строка 219**: Реализовать логику получения следующего вопроса
```java
// TODO: Реализовать логику получения следующего вопроса
// Реализовать:
public Question getNextQuestion(Long interviewId) {
    // 1. Получить текущий прогресс интервью
    // 2. Определить следующий вопрос
    // 3. Вернуть вопрос или null если интервью завершено
}
```

---

## 📋 ПЛАН ВЫПОЛНЕНИЯ

### Этап 1: Критические исправления (1-2 дня)

1. **День 1**:
   - Исправить JWT secret
   - Исправить API ключи
   - Создать .env файлы

2. **День 2**:
   - Исправить CORS настройки
   - Протестировать безопасность

### Этап 2: Средние исправления (3-5 дней)

3. **День 3-4**:
   - Добавить валидацию в контроллеры
   - Исправить логирование в тестах

4. **День 5**:
   - Реализовать критичные TODO
   - Протестировать функциональность

### Этап 3: Тестирование (1-2 дня)

5. **День 6-7**:
   - Полное тестирование
   - Проверка безопасности
   - Документирование изменений

---

## ✅ КРИТЕРИИ ПРИЕМКИ

### Критические исправления:
- [ ] JWT secret вынесен в переменные окружения
- [ ] API ключи не содержат тестовых значений
- [ ] CORS настройки ограничены конкретными доменами
- [ ] Все изменения протестированы

### Средние исправления:
- [ ] Все контроллеры используют `@Valid`
- [ ] Логирование в тестах заменено на proper logging
- [ ] Критичные TODO реализованы
- [ ] Функциональность протестирована

### Общие требования:
- [ ] Проект собирается без ошибок
- [ ] Все тесты проходят
- [ ] Документация обновлена
- [ ] Код соответствует стандартам проекта

---

## 🛡️ ТРЕБОВАНИЯ К БЕЗОПАСНОСТИ

1. **Никаких секретов в коде**
2. **Валидация всех входных данных**
3. **Ограниченные CORS настройки**
4. **Безопасные дефолтные значения**
5. **Логирование без чувствительных данных**

---

## 📝 ДОКУМЕНТАЦИЯ

После выполнения всех исправлений необходимо обновить:

1. **README.md** - инструкции по настройке переменных окружения
2. **.env.example** - пример конфигурации
3. **Документация по развертыванию** - обновить инструкции
4. **CHANGELOG.md** - записать все изменения

---

**Статус**: 🚀 **ГОТОВО К ВЫПОЛНЕНИЮ**

**Приоритет**: 🔥 **КРИТИЧЕСКИЙ** - требует немедленного выполнения 