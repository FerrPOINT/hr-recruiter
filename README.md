# 🎤 HR Recruiter Backend с ElevenLabs AI

Современная платформа для автоматизации процесса найма с интеграцией ElevenLabs Conversational AI для проведения голосовых интервью.

## 🚀 Быстрый старт

### Предварительные требования

- **Java 21** или выше
- **PostgreSQL 14** или выше
- **Redis** (опционально, для кэширования)
- **ElevenLabs API ключ** (получить на [elevenlabs.io](https://elevenlabs.io))

### Установка и запуск

1. **Клонирование репозитория:**
```bash
git clone https://github.com/your-org/hr-recruiter-back.git
cd hr-recruiter-back
```

2. **Настройка базы данных:**
```sql
CREATE DATABASE hr_recruiter;
CREATE USER hr_user WITH PASSWORD 'hr_password';
GRANT ALL PRIVILEGES ON DATABASE hr_recruiter TO hr_user;
```

3. **Настройка переменных окружения:**
```bash
cp env.example .env
```

Отредактируйте `.env` файл:
```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/hr_recruiter
DB_USERNAME=hr_user
DB_PASSWORD=hr_password

# JWT
JWT_SECRET=your-super-secret-jwt-key-here-make-it-long-and-secure

# ElevenLabs
ELEVEN_LABS_API_KEY=your-elevenlabs-api-key-here

# AI Services
OPENROUTER_API_KEY=your-openrouter-api-key-here
```

4. **Запуск приложения:**
```bash
./gradlew bootRun
```

Приложение будет доступно по адресу: http://localhost:8080

## 📚 Документация API

### Swagger UI
Интерактивная документация доступна по адресу: http://localhost:8080/swagger-ui.html

### OpenAPI спецификация
Полная спецификация API: http://localhost:8080/v3/api-docs

## 🔐 Аутентификация

### Для администраторов (HR)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password123"
  }'
```

### Для кандидатов
```bash
curl -X POST http://localhost:8080/api/v1/candidates/auth \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Иван",
    "lastName": "Иванов",
    "email": "ivan@example.com",
    "phone": "+79991234567",
    "positionId": 123
  }'
```

## 🎤 Голосовые интервью

### Создание голосового интервью

1. **Создание вакансии:**
```bash
curl -X POST http://localhost:8080/api/v1/positions \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Java Developer",
    "description": "Разработчик Java",
    "level": "middle",
    "language": "ru"
  }'
```

2. **Добавление вопросов:**
```bash
curl -X POST http://localhost:8080/api/v1/positions/1/questions \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Расскажите о принципах SOLID",
    "type": "text",
    "order": 1
  }'
```

3. **Запуск голосового интервью:**
```bash
curl -X POST http://localhost:8080/api/v1/interviews/1/start \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "voiceMode": true,
    "autoCreateAgent": true
  }'
```

### Интеграция с фронтендом

Для интеграции с React/JavaScript используйте SDK ElevenLabs:

```javascript
import { ElevenLabs } from '@elevenlabs/react';

const VoiceInterview = () => {
  const [session, setSession] = useState(null);

  const startInterview = async () => {
    const response = await fetch('/api/v1/interviews/1/voice/session', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        voiceMode: true,
        autoCreateAgent: true
      })
    });
    
    const sessionData = await response.json();
    setSession(sessionData);
  };

  return (
    <div>
      <button onClick={startInterview}>Начать интервью</button>
      {session && (
        <ElevenLabs
          sessionId={session.sessionId}
          onMessage={(message) => console.log('Получено сообщение:', message)}
        />
      )}
    </div>
  );
};
```

## 🏗️ Архитектура

### Основные компоненты

- **AuthController** - аутентификация и авторизация
- **InterviewsApiController** - управление интервью
- **VoiceInterviewController** - голосовые интервью
- **AgentsApiController** - управление AI агентами
- **WebhookController** - обработка событий ElevenLabs

### Сервисы

- **JwtService** - работа с JWT токенами
- **VoiceInterviewService** - управление голосовыми сессиями
- **ElevenLabsAgentService** - создание и управление агентами
- **WebhookService** - обработка webhook событий

### База данных

- **users** - пользователи системы
- **positions** - вакансии
- **candidates** - кандидаты
- **interviews** - интервью
- **questions** - вопросы
- **agents** - AI агенты

## 🔧 Конфигурация

### application.yaml

```yaml
# ElevenLabs Configuration
elevenlabs:
  api:
    key: ${ELEVEN_LABS_API_KEY}
    url: https://api.elevenlabs.io
    timeout: 30000
  webhook:
    secret: ${ELEVENLABS_WEBHOOK_SECRET}
  default:
    agent-id: your-default-agent-id
    voice-id: 21m00Tcm4TlvDq8ikWAM
    language: ru
    prompt: "Ты проводишь собеседование..."

# Security
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 24h

# Database
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## 🧪 Тестирование

### Запуск тестов
```bash
./gradlew test
```

### Интеграционные тесты
```bash
./gradlew integrationTest
```

### Проверка качества кода
```bash
./gradlew sonarqube
```

## 📊 Мониторинг

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Метрики
```bash
curl http://localhost:8080/actuator/metrics
```

### Логи
Логи доступны в консоли и файлах:
- `logs/application.log` - основные логи
- `logs/error.log` - ошибки

## 🚀 Развертывание

### Docker
```bash
docker build -t hr-recruiter-back .
docker run -p 8080:8080 hr-recruiter-back
```

### Docker Compose
```bash
docker-compose up -d
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

## 🔒 Безопасность

### JWT Токены
- Алгоритм: HMAC-SHA256
- Время жизни: 24 часа (настраивается)
- Claims: role, userId, email

### Webhook Безопасность
- HMAC-SHA256 валидация подписей
- Защита от replay атак
- Валидация источника

### Авторизация
- Роли: ADMIN, CANDIDATE
- Метод-уровень: @PreAuthorize
- Endpoint-уровень: Spring Security

## 📈 Производительность

### Оптимизации
- Connection pooling (HikariCP)
- Кэширование (Redis)
- Асинхронная обработка webhook
- Batch операции

### Масштабируемость
- Stateless архитектура
- Горизонтальное масштабирование
- Load balancing готовность

## 🤝 Поддержка

### Документация
- [Руководство по авторизации](info/AUTH_GUIDE.md)
- [Техническая документация](info/BACKEND_IMPLEMENTATION_SUMMARY.md)
- [Отчет по интеграции ElevenLabs](info/ELEVENLABS_INTEGRATION_COMPLETION_REPORT.md)

### Контакты
- Email: support@hr-recruiter.com
- Issues: [GitHub Issues](https://github.com/your-org/hr-recruiter-back/issues)

## 📄 Лицензия

MIT License - см. файл [LICENSE](LICENSE)

---

**🎉 Готово к использованию!**

Система полностью готова для автоматизации процесса найма с использованием современных AI технологий.