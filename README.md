# HR Recruiter Backend

Spring Boot REST API для платформы автоматизации HR-собеседований.

## Быстрый запуск с Docker

```bash
# Запустить все сервисы
docker-compose up -d

# Посмотреть логи
docker-compose logs -f app

# Остановить
docker-compose down
```

## Ручной запуск

### Требования
- Java 21
- PostgreSQL 16
- Redis 7

### Настройка базы данных
```sql
CREATE DATABASE hr_recruiter;
CREATE USER hr_user WITH PASSWORD 'hr_password';
GRANT ALL PRIVILEGES ON DATABASE hr_recruiter TO hr_user;
```

### Запуск приложения
```bash
./gradlew bootRun
```

## API документация

После запуска доступна по адресу: http://localhost:8080/api/swagger-ui.html

## Тестирование

```bash
./gradlew test
```

## 🚀 Особенности

- **Современная архитектура**: Spring Boot 3.2, Java 21, PostgreSQL
- **AI интеграция**: Claude AI API для генерации контента и анализа
- **Чистая архитектура**: SOLID принципы, DDD подходы
- **REST API**: Полная OpenAPI 3.0 спецификация
- **Безопасность**: Spring Security с JWT
- **Автоматизация**: Lombok, MapStruct, OpenAPI Generator
- **Контейнеризация**: Docker и Docker Compose

## 🏗️ Архитектура

### AI Модуль

Профессиональная архитектура AI модуля следует принципам SOLID и DDD:

```
ai/
├── AIService.java              # Интерфейс для AI сервисов
├── AIServiceException.java     # Кастомные исключения
├── AIUsageStats.java          # Статистика использования
├── ClaudeService.java         # Реализация Claude AI
├── dto/                       # DTO для API запросов/ответов
│   ├── ClaudeRequest.java
│   ├── ClaudeResponse.java
│   ├── ClaudeMessage.java
│   ├── ClaudeContent.java
│   ├── ClaudeUsage.java
│   ├── ClaudeMetadata.java
│   └── ClaudeMessageMetadata.java
└── config/
    └── ClaudeConfig.java      # Конфигурация Claude
```

### Сервисы

- **ClaudeService**: Полнофункциональный сервис для работы с Claude AI API
- **RewriteService**: Сервис для переписывания и улучшения текстов
- **WhisperService**: Сервис для транскрипции аудио

## 🔧 Технологический стек

### Основные технологии
- **Java 21** - Последняя версия Java
- **Spring Boot 3.2** - Современный фреймворк
- **PostgreSQL** - Надежная СУБД
- **Gradle** - Система сборки

### AI интеграция
- **Claude AI API** - Основной AI провайдер
- **RestTemplate** - HTTP клиент для API вызовов
- **Jackson** - JSON сериализация/десериализация

### Автоматизация
- **Lombok** - Уменьшение boilerplate кода
- **MapStruct** - Маппинг между объектами
- **OpenAPI Generator** - Автогенерация кода

## 🚀 Быстрый старт

### Предварительные требования
- Java 21+
- Docker и Docker Compose
- Gradle 8.0+

### Запуск с Docker

```bash
# Клонирование репозитория
git clone <repository-url>
cd hr-recruiter-back

# Запуск с Docker Compose
docker-compose up -d

# Приложение будет доступно на http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Локальная разработка

```bash
# Установка зависимостей
./gradlew build

# Запуск приложения
./gradlew bootRun

# Запуск тестов
./gradlew test
```

## 🔑 Конфигурация

### Переменные окружения

```bash
# Claude AI API
ANTHROPIC_API_KEY=your-claude-api-key
ANTHROPIC_MODEL=claude-3-sonnet-20240229
ANTHROPIC_MAX_TOKENS=1000

# База данных
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hr_recruiter
DB_USER=postgres
DB_PASSWORD=password

# Приложение
SERVER_PORT=8080
JWT_SECRET=your-jwt-secret
```

### application.yaml

```yaml
anthropic:
  api:
    key: ${ANTHROPIC_API_KEY:sk-demo-key}
    url: https://api.anthropic.com/v1/messages
  model: claude-3-sonnet-20240229
  max-tokens: 1000
  temperature: 0.7
  timeout: 30000
  enable-prompt-caching: true
  enable-usage-metrics: true
```

## 📚 API Документация

### AI Endpoints

#### Основные AI функции
- `POST /api/ai/generate` - Генерация текста
- `POST /api/ai/generate-list` - Генерация списка текстов
- `GET /api/ai/health` - Проверка доступности AI
- `GET /api/ai/stats` - Статистика использования

#### Переписывание текстов
- `POST /api/ai/rewrite/professional` - Профессиональный стиль
- `POST /api/ai/rewrite/casual` - Неформальный стиль
- `POST /api/ai/rewrite/formal` - Формальный стиль
- `POST /api/ai/rewrite/concise` - Краткий стиль
- `POST /api/ai/rewrite/custom` - Кастомные параметры
- `POST /api/ai/rewrite/multiple` - Множественные стили

#### Специализированные функции
- `POST /api/ai/improve/job-description` - Улучшение описания вакансии
- `POST /api/ai/improve/email` - Улучшение email
- `POST /api/ai/improve/company-description` - Улучшение описания компании
- `POST /api/ai/improve/interview-answer` - Улучшение ответа на интервью
- `POST /api/ai/alternatives` - Генерация альтернатив
- `POST /api/ai/grammar-check` - Проверка грамматики

### Примеры использования

#### Генерация текста
```bash
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Напиши профессиональное описание вакансии Java разработчика"}'
```

#### Переписывание текста
```bash
curl -X POST http://localhost:8080/api/ai/rewrite/professional \
  -H "Content-Type: application/json" \
  -d '{"text": "Мы ищем программиста для работы с кодом"}'
```

## 🏛️ Архитектурные принципы

### SOLID принципы

1. **Single Responsibility (SRP)**: Каждый класс имеет одну ответственность
2. **Open/Closed (OCP)**: Классы открыты для расширения, закрыты для модификации
3. **Liskov Substitution (LSP)**: Подклассы могут заменять базовые классы
4. **Interface Segregation (ISP)**: Клиенты не зависят от неиспользуемых методов
5. **Dependency Inversion (DIP)**: Зависимости от абстракций, а не от конкретных классов

### DDD подходы

- **Domain Entities**: Бизнес-сущности (User, Position, Candidate)
- **Value Objects**: Неизменяемые объекты (Email, Phone)
- **Aggregates**: Группы связанных сущностей
- **Repositories**: Доступ к данным
- **Services**: Бизнес-логика
- **Factories**: Создание сложных объектов

### Design Patterns

- **Strategy Pattern**: Разные стратегии AI провайдеров
- **Factory Pattern**: Создание AI сервисов
- **Builder Pattern**: Построение сложных запросов
- **Observer Pattern**: Логирование и метрики
- **Template Method**: Базовые алгоритмы AI

## 🔍 Мониторинг и логирование

### Логирование
- Структурированные логи с использованием SLF4J
- Разные уровни логирования для разных окружений
- Логирование всех AI запросов и ответов

### Метрики
- Статистика использования AI сервисов
- Время ответа и количество токенов
- Успешность запросов и ошибки

### Health Checks
- Проверка доступности AI сервисов
- Проверка состояния базы данных
- Проверка внешних зависимостей

## 🧪 Тестирование

### Unit тесты
```bash
./gradlew test
```

### Интеграционные тесты
```bash
./gradlew integrationTest
```

### Тестирование AI
```bash
./gradlew test --tests "*AIServiceTest"
```

## 🚀 Деплой

### Docker
```bash
# Сборка образа
docker build -t hr-recruiter-back .

# Запуск контейнера
docker run -p 8080:8080 hr-recruiter-back
```

### Docker Compose
```bash
# Полный стек
docker-compose up -d

# Только приложение
docker-compose up app
```

## 📈 Производительность

### Оптимизации
- Кэширование промптов
- Асинхронная обработка
- Connection pooling
- Пагинация результатов

### Мониторинг
- Micrometer метрики
- Prometheus экспорт
- Grafana дашборды

## 🔒 Безопасность

- Spring Security с JWT
- Валидация входных данных
- Rate limiting
- CORS настройки
- Безопасное хранение API ключей

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch
3. Внесите изменения
4. Добавьте тесты
5. Создайте Pull Request

## 📄 Лицензия

MIT License

## 📞 Поддержка

- Email: support@hr-recruiter.com
- Документация: [docs.hr-recruiter.com](https://docs.hr-recruiter.com)
- Issues: [GitHub Issues](https://github.com/your-org/hr-recruiter-back/issues)
