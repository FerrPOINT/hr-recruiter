# 🎤 Backend Development Guide: Speech-to-Speech Interview System

## 📋 Обзор

Документ описывает необходимые доработки бэкенда для полноценной работы speech-to-speech интервью с использованием ElevenLabs Conversational AI. Основной фокус - автоматическое создание агентов и управление голосовыми сессиями.

---

## 🎯 Основные задачи

### 1. Доработка метода `/interviews/{id}/start`
### 2. Автоматическое создание агентов ElevenLabs
### 3. Расширение OpenAPI схемы
### 4. Интеграция с ElevenLabs API
### 5. Webhook обработчики
### 6. Модели данных для агентов и сессий

---

## 🔧 Доработка метода `/interviews/{id}/start`

### Текущее состояние:
```yaml
/interviews/{id}/start:
  post:
    operationId: startInterview
    summary: Начать интервью
    responses:
      '200':
        description: Интервью успешно начато
```

### Необходимые изменения:

#### 1. Расширенный запрос
```yaml
/interviews/{id}/start:
  post:
    operationId: startInterview
    summary: Начать интервью (с поддержкой голосового режима)
    requestBody:
      required: false
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/InterviewStartRequest'
    responses:
      '200':
        description: Интервью успешно начато
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/InterviewStartResponse'
      '400':
        description: Некорректные параметры
      '404':
        description: Интервью не найдено
      '500':
        description: Ошибка создания агента
```

#### 2. Новые схемы данных
```yaml
InterviewStartRequest:
  type: object
  properties:
    voiceMode:
      type: boolean
      description: Включить голосовой режим
      default: false
    agentConfig:
      $ref: '#/components/schemas/AgentConfig'
    voiceSettings:
      $ref: '#/components/schemas/VoiceSettings'

InterviewStartResponse:
  type: object
  properties:
    interviewId:
      type: integer
      format: int64
    agentId:
      type: string
      description: ID созданного агента в ElevenLabs
    sessionId:
      type: string
      description: ID голосовой сессии
    status:
      type: string
      enum: [started, agent_created, error]
    message:
      type: string
      description: Дополнительная информация
```

---

## 🤖 Автоматическое создание агентов

### 1. Сервис для работы с агентами

```java
@Service
@Slf4j
public class ElevenLabsAgentService {
    
    private final ElevenLabsClient elevenLabsClient;
    private final AgentRepository agentRepository;
    private final PositionRepository positionRepository;
    
    /**
     * Создает агента для интервью на основе позиции
     */
    public Agent createAgentForInterview(Interview interview) {
        Position position = positionRepository.findById(interview.getPositionId())
            .orElseThrow(() -> new NotFoundException("Position not found"));
            
        // Создаем конфигурацию агента
        AgentConfig config = buildAgentConfig(position, interview);
        
        // Создаем агента в ElevenLabs
        String elevenLabsAgentId = createElevenLabsAgent(config);
        
        // Сохраняем в базу данных
        Agent agent = new Agent();
        agent.setInterviewId(interview.getId());
        agent.setElevenLabsAgentId(elevenLabsAgentId);
        agent.setConfig(config);
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setCreatedAt(LocalDateTime.now());
        
        return agentRepository.save(agent);
    }
    
    /**
     * Строит конфигурацию агента на основе позиции
     */
    private AgentConfig buildAgentConfig(Position position, Interview interview) {
        return AgentConfig.builder()
            .name("Interview Agent - " + position.getTitle())
            .description("AI interviewer for " + position.getTitle() + " position")
            .prompt(buildInterviewPrompt(position))
            .voiceId(selectVoiceForPosition(position))
            .tools(Arrays.asList("getNextQuestion", "saveAnswer", "endInterview"))
            .webhookUrl(buildWebhookUrl(interview.getId()))
            .build();
    }
    
    /**
     * Создает промпт для агента на основе позиции
     */
    private String buildInterviewPrompt(Position position) {
        return String.format("""
            Ты проводишь собеседование на позицию "%s".
            
            Инструкции:
            1. Используй инструмент getNextQuestion для получения вопросов
            2. Озвучивай вопросы четко и профессионально
            3. Дождись ответа кандидата
            4. Используй saveAnswer для сохранения ответа
            5. Заверши интервью после последнего вопроса
            
            Позиция: %s
            Уровень: %s
            Язык: %s
            """, 
            position.getTitle(),
            position.getDescription(),
            position.getLevel(),
            position.getLanguage()
        );
    }
    
    /**
     * Выбирает голос для позиции
     */
    private String selectVoiceForPosition(Position position) {
        // Логика выбора голоса на основе языка и уровня позиции
        if ("ru".equals(position.getLanguage())) {
            return position.getLevel() == PositionLevel.SENIOR ? 
                "21m00Tcm4TlvDq8ikWAM" : // Rachel для senior
                "pNInz6obpgDQGcFmaJgB";   // Adam для остальных
        } else {
            return "EXAVITQu4vr4xnSDxMaL"; // Bella для английского
        }
    }
    
    /**
     * Создает агента в ElevenLabs API
     */
    private String createElevenLabsAgent(AgentConfig config) {
        try {
            CreateAgentRequest request = CreateAgentRequest.builder()
                .name(config.getName())
                .description(config.getDescription())
                .prompt(config.getPrompt())
                .voiceId(config.getVoiceId())
                .tools(config.getTools())
                .webhookUrl(config.getWebhookUrl())
                .build();
                
            CreateAgentResponse response = elevenLabsClient.createAgent(request);
            log.info("Created ElevenLabs agent: {}", response.getAgentId());
            
            return response.getAgentId();
        } catch (Exception e) {
            log.error("Failed to create ElevenLabs agent", e);
            throw new RuntimeException("Failed to create agent", e);
        }
    }
}
```

### 2. Модели данных для агентов

```java
@Entity
@Table(name = "agents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "interview_id")
    private Long interviewId;
    
    @Column(name = "elevenlabs_agent_id")
    private String elevenLabsAgentId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AgentStatus status;
    
    @Column(name = "config", columnDefinition = "JSON")
    @Type(JsonType.class)
    private AgentConfig config;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {
    private String name;
    private String description;
    private String prompt;
    private String voiceId;
    private List<String> tools;
    private String webhookUrl;
    private VoiceSettings voiceSettings;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSettings {
    private Double stability;
    private Double similarityBoost;
    private Double style;
    private Boolean useSpeakerBoost;
}

public enum AgentStatus {
    ACTIVE, INACTIVE, DELETED, ERROR
}
```

---

## 🔗 Webhook обработчики

### 1. Контроллер для webhook'ов

```java
@RestController
@RequestMapping("/api/v1/webhooks/elevenlabs")
@Slf4j
public class ElevenLabsWebhookController {
    
    private final InterviewService interviewService;
    private final AgentService agentService;
    
    /**
     * Обработчик событий от ElevenLabs
     */
    @PostMapping("/events")
    public ResponseEntity<Void> handleElevenLabsEvent(
            @RequestBody ElevenLabsWebhookEvent event,
            @RequestHeader("X-ElevenLabs-Signature") String signature) {
        
        try {
            // Валидация подписи
            if (!validateSignature(event, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Received ElevenLabs webhook: {}", event.getType());
            
            switch (event.getType()) {
                case "agent.message":
                    handleAgentMessage(event);
                    break;
                case "agent.tool_call":
                    handleAgentToolCall(event);
                    break;
                case "conversation.ended":
                    handleConversationEnded(event);
                    break;
                default:
                    log.warn("Unknown webhook event type: {}", event.getType());
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Обработка сообщений от агента
     */
    private void handleAgentMessage(ElevenLabsWebhookEvent event) {
        AgentMessageEvent messageEvent = (AgentMessageEvent) event;
        
        // Сохраняем сообщение агента
        interviewService.saveAgentMessage(
            messageEvent.getInterviewId(),
            messageEvent.getMessage(),
            messageEvent.getTimestamp()
        );
    }
    
    /**
     * Обработка вызовов инструментов агентом
     */
    private void handleAgentToolCall(ElevenLabsWebhookEvent event) {
        AgentToolCallEvent toolEvent = (AgentToolCallEvent) event;
        
        switch (toolEvent.getToolName()) {
            case "getNextQuestion":
                handleGetNextQuestion(toolEvent);
                break;
            case "saveAnswer":
                handleSaveAnswer(toolEvent);
                break;
            case "endInterview":
                handleEndInterview(toolEvent);
                break;
        }
    }
    
    /**
     * Обработка завершения разговора
     */
    private void handleConversationEnded(ElevenLabsWebhookEvent event) {
        ConversationEndedEvent endedEvent = (ConversationEndedEvent) event;
        
        interviewService.finishVoiceInterview(
            endedEvent.getInterviewId(),
            endedEvent.getReason()
        );
    }
}
```

### 2. Модели webhook событий

```java
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentMessageEvent.class, name = "agent.message"),
    @JsonSubTypes.Type(value = AgentToolCallEvent.class, name = "agent.tool_call"),
    @JsonSubTypes.Type(value = ConversationEndedEvent.class, name = "conversation.ended")
})
public abstract class ElevenLabsWebhookEvent {
    private String type;
    private String interviewId;
    private LocalDateTime timestamp;
}

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentMessageEvent extends ElevenLabsWebhookEvent {
    private String message;
    private String agentId;
}

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentToolCallEvent extends ElevenLabsWebhookEvent {
    private String toolName;
    private Map<String, Object> parameters;
    private String result;
}

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationEndedEvent extends ElevenLabsWebhookEvent {
    private String reason;
    private Map<String, Object> metadata;
}
```

---

## 📊 Расширение моделей данных

### 1. Расширение модели Interview

```java
@Entity
@Table(name = "interviews")
public class Interview {
    // ... существующие поля
    
    @Column(name = "voice_mode")
    private Boolean voiceMode = false;
    
    @Column(name = "agent_id")
    private String agentId;
    
    @Column(name = "voice_session_id")
    private String voiceSessionId;
    
    @Column(name = "voice_settings", columnDefinition = "JSON")
    @Type(JsonType.class)
    private VoiceSettings voiceSettings;
    
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    private List<VoiceMessage> voiceMessages;
    
    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private Agent agent;
}

@Entity
@Table(name = "voice_messages")
@Data
public class VoiceMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "interview_id")
    private Long interviewId;
    
    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private VoiceMessageType type;
    
    @Column(name = "text")
    private String text;
    
    @Column(name = "audio_url")
    private String audioUrl;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "confidence")
    private Double confidence;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> metadata;
}

public enum VoiceMessageType {
    AGENT_QUESTION, USER_ANSWER, AGENT_MESSAGE, SYSTEM_MESSAGE
}
```

### 2. Расширение модели InterviewAnswer

```java
@Entity
@Table(name = "interview_answers")
public class InterviewAnswer {
    // ... существующие поля
    
    @Column(name = "voice_generated")
    private Boolean voiceGenerated = false;
    
    @Column(name = "audio_quality")
    private Double audioQuality;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "voice_metadata", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> voiceMetadata;
}
```

---

## 🔧 Интеграция с ElevenLabs API

### 1. Клиент для ElevenLabs API

```java
@Component
@Slf4j
public class ElevenLabsClient {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public ElevenLabsClient(@Value("${elevenlabs.api.key}") String apiKey,
                           @Value("${elevenlabs.api.url}") String baseUrl) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("xi-api-key", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    /**
     * Создает агента
     */
    public CreateAgentResponse createAgent(CreateAgentRequest request) {
        return webClient.post()
            .uri("/v1/agents")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CreateAgentResponse.class)
            .block();
    }
    
    /**
     * Создает голосовую сессию
     */
    public CreateSessionResponse createSession(CreateSessionRequest request) {
        return webClient.post()
            .uri("/v1/conversations/sessions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CreateSessionResponse.class)
            .block();
    }
    
    /**
     * Удаляет агента
     */
    public void deleteAgent(String agentId) {
        webClient.delete()
            .uri("/v1/agents/{agentId}", agentId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
    
    /**
     * Получает список доступных голосов
     */
    public List<Voice> getVoices() {
        return webClient.get()
            .uri("/v1/voices")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Voice>>() {})
            .block();
    }
}

@Data
@Builder
public class CreateAgentRequest {
    private String name;
    private String description;
    private String prompt;
    private String voiceId;
    private List<String> tools;
    private String webhookUrl;
    private Map<String, Object> settings;
}

@Data
public class CreateAgentResponse {
    private String agentId;
    private String status;
    private String message;
}

@Data
@Builder
public class CreateSessionRequest {
    private String agentId;
    private String conversationId;
    private Map<String, Object> settings;
}

@Data
public class CreateSessionResponse {
    private String sessionId;
    private String status;
    private String message;
}
```

---

## 📋 Обновленная OpenAPI схема

### 1. Новые эндпоинты

```yaml
# Управление агентами
/agents:
  get:
    operationId: listAgents
    tags: [Agents]
    summary: Получить список агентов
    security: [AdminAuth]
    responses:
      '200':
        description: OK
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AgentsPaginatedResponse
  post:
    operationId: createAgent
    tags: [Agents]
    summary: Создать агента
    security: [AdminAuth]
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/AgentCreateRequest'
    responses:
      '201':
        description: Агент создан
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Agent'

/agents/{id}:
  get:
    operationId: getAgent
    tags: [Agents]
    summary: Получить агента
    security: [AdminAuth]
    parameters:
      - in: path
        name: id
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '200':
        description: OK
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Agent'
  delete:
    operationId: deleteAgent
    tags: [Agents]
    summary: Удалить агента
    security: [AdminAuth]
    parameters:
      - in: path
        name: id
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '204':
        description: Агент удален

# Webhook обработчики
/webhooks/elevenlabs/events:
  post:
    operationId: handleElevenLabsWebhook
    tags: [Webhooks]
    summary: Обработчик событий ElevenLabs
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ElevenLabsWebhookEvent'
    responses:
      '200':
        description: Событие обработано
      '400':
        description: Некорректные данные
      '500':
        description: Ошибка обработки

# Расширенный старт интервью
/interviews/{id}/start:
  post:
    operationId: startInterview
    tags: [Interviews]
    summary: Начать интервью (с поддержкой голосового режима)
    security: [AdminAuth]
    parameters:
      - in: path
        name: id
        required: true
        schema:
          type: integer
          format: int64
    requestBody:
      required: false
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/InterviewStartRequest'
    responses:
      '200':
        description: Интервью успешно начато
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/InterviewStartResponse'
```

### 2. Новые схемы данных

```yaml
# Агенты
Agent:
  type: object
  properties:
    id:
      type: integer
      format: int64
    interviewId:
      type: integer
      format: int64
    elevenLabsAgentId:
      type: string
    name:
      type: string
    status:
      $ref: '#/components/schemas/AgentStatus'
    config:
      $ref: '#/components/schemas/AgentConfig'
    createdAt:
      type: string
      format: date-time
    updatedAt:
      type: string
      format: date-time
  required: [id, interviewId, elevenLabsAgentId, name, status, createdAt]

AgentStatus:
  type: string
  enum: [ACTIVE, INACTIVE, DELETED, ERROR]

AgentConfig:
  type: object
  properties:
    name:
      type: string
    description:
      type: string
    prompt:
      type: string
    voiceId:
      type: string
    tools:
      type: array
      items:
        type: string
    webhookUrl:
      type: string
    voiceSettings:
      $ref: '#/components/schemas/VoiceSettings'

VoiceSettings:
  type: object
  properties:
    stability:
      type: number
      format: float
      minimum: 0
      maximum: 1
    similarityBoost:
      type: number
      format: float
      minimum: 0
      maximum: 1
    style:
      type: number
      format: float
      minimum: 0
      maximum: 1
    useSpeakerBoost:
      type: boolean

# Запросы и ответы
InterviewStartRequest:
  type: object
  properties:
    voiceMode:
      type: boolean
      default: false
    agentConfig:
      $ref: '#/components/schemas/AgentConfig'
    voiceSettings:
      $ref: '#/components/schemas/VoiceSettings'

InterviewStartResponse:
  type: object
  properties:
    interviewId:
      type: integer
      format: int64
    agentId:
      type: string
    sessionId:
      type: string
    status:
      type: string
      enum: [started, agent_created, error]
    message:
      type: string

AgentCreateRequest:
  type: object
  properties:
    interviewId:
      type: integer
      format: int64
    config:
      $ref: '#/components/schemas/AgentConfig'
  required: [interviewId, config]

# Webhook события
ElevenLabsWebhookEvent:
  type: object
  properties:
    type:
      type: string
      enum: [agent.message, agent.tool_call, conversation.ended]
    interviewId:
      type: string
    timestamp:
      type: string
      format: date-time
    data:
      type: object
  required: [type, interviewId, timestamp]

# Голосовые сообщения
VoiceMessage:
  type: object
  properties:
    id:
      type: integer
      format: int64
    interviewId:
      type: integer
      format: int64
    type:
      $ref: '#/components/schemas/VoiceMessageType'
    text:
      type: string
    audioUrl:
      type: string
    durationMs:
      type: integer
      format: int64
    confidence:
      type: number
      format: float
    timestamp:
      type: string
      format: date-time
    metadata:
      type: object
  required: [id, interviewId, type, text, timestamp]

VoiceMessageType:
  type: string
  enum: [AGENT_QUESTION, USER_ANSWER, AGENT_MESSAGE, SYSTEM_MESSAGE]

# Пагинированные ответы
AgentsPaginatedResponse:
  $ref: '#/components/schemas/PaginatedResponse'
  description: "Пагинированный список агентов"
  properties:
    content:
      type: array
      items:
        $ref: '#/components/schemas/Agent'
```

---

## 🚀 План реализации

### Этап 1: Базовая инфраструктура (1-2 недели)
1. ✅ Создать модели данных (Agent, VoiceMessage, AgentConfig)
2. ✅ Реализовать ElevenLabsClient
3. ✅ Добавить репозитории для новых сущностей
4. ✅ Создать базовые сервисы

### Этап 2: Автоматическое создание агентов (1 неделя)
1. ✅ Реализовать ElevenLabsAgentService
2. ✅ Добавить логику создания промптов
3. ✅ Интегрировать с методом startInterview
4. ✅ Добавить обработку ошибок

### Этап 3: Webhook обработчики (1 неделя)
1. ✅ Создать ElevenLabsWebhookController
2. ✅ Реализовать обработку событий
3. ✅ Добавить валидацию подписей
4. ✅ Интегрировать с существующими сервисами

### Этап 4: Расширение API (1 неделя)
1. ✅ Обновить OpenAPI схему
2. ✅ Добавить новые эндпоинты
3. ✅ Расширить существующие модели
4. ✅ Добавить валидацию

### Этап 5: Тестирование и интеграция (1 неделя)
1. ✅ Создать интеграционные тесты
2. ✅ Протестировать полный цикл
3. ✅ Настроить мониторинг
4. ✅ Документировать API

---

## 🔒 Безопасность

### 1. Валидация webhook подписей
```java
private boolean validateSignature(ElevenLabsWebhookEvent event, String signature) {
    String payload = objectMapper.writeValueAsString(event);
    String expectedSignature = generateHmacSha256(payload, webhookSecret);
    return MessageDigest.isEqual(
        signature.getBytes(StandardCharsets.UTF_8),
        expectedSignature.getBytes(StandardCharsets.UTF_8)
    );
}
```

### 2. Безопасное хранение API ключей
```yaml
# application.yml
elevenlabs:
  api:
    key: ${ELEVENLABS_API_KEY}
    url: ${ELEVENLABS_API_URL:https://api.elevenlabs.io}
  webhook:
    secret: ${ELEVENLABS_WEBHOOK_SECRET}
```

### 3. Ограничение доступа к агентам
```java
@PreAuthorize("hasRole('ADMIN') or @interviewService.isInterviewOwner(#agentId, authentication)")
public Agent getAgent(Long agentId) {
    return agentRepository.findById(agentId)
        .orElseThrow(() -> new NotFoundException("Agent not found"));
}
```

---

## 📊 Мониторинг и логирование

### 1. Метрики для отслеживания
```java
@Component
public class AgentMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordAgentCreation(String positionLevel, String language) {
        meterRegistry.counter("agent.creation", 
            "level", positionLevel, 
            "language", language).increment();
    }
    
    public void recordWebhookEvent(String eventType, String status) {
        meterRegistry.counter("webhook.events",
            "type", eventType,
            "status", status).increment();
    }
}
```

### 2. Структурированное логирование
```java
@Slf4j
public class ElevenLabsAgentService {
    
    public Agent createAgentForInterview(Interview interview) {
        log.info("Creating agent for interview", 
            "interviewId", interview.getId(),
            "positionId", interview.getPositionId(),
            "voiceMode", interview.getVoiceMode());
            
        try {
            // ... логика создания
            log.info("Agent created successfully",
                "agentId", agent.getId(),
                "elevenLabsAgentId", agent.getElevenLabsAgentId());
            return agent;
        } catch (Exception e) {
            log.error("Failed to create agent",
                "interviewId", interview.getId(),
                "error", e.getMessage());
            throw e;
        }
    }
}
```

---

## 🎯 Заключение

Данный документ описывает полную архитектуру для интеграции speech-to-speech интервью с ElevenLabs. Основные компоненты:

1. **Автоматическое создание агентов** - агенты создаются автоматически при запуске голосового интервью
2. **Webhook обработчики** - обработка событий от ElevenLabs в реальном времени
3. **Расширенные модели данных** - поддержка агентов, голосовых сообщений и настроек
4. **Безопасность** - валидация подписей и контроль доступа
5. **Мониторинг** - метрики и логирование для отслеживания работы

Реализация позволит создать полноценную систему голосовых интервью с минимальным участием пользователя в настройке и управлении. 