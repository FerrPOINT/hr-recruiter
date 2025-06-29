# Конфигурация логирования HTTP запросов

## Обзор

В приложении настроено логирование всех входящих HTTP запросов и исходящих ответов. Логирование включает:

- Метод HTTP запроса
- URL и query параметры
- IP адрес клиента
- User-Agent
- Статус код ответа
- Время выполнения запроса
- Размер ответа
- Content-Type ответа

## Конфигурация

### Основное логирование

Основное логирование настроено в классе `LoggingConfig` и включает:

1. **RequestLoggingFilter** - основной фильтр для логирования
2. **RequestId** - уникальный идентификатор для каждого запроса
3. **Время выполнения** - измерение времени обработки запроса
4. **Фильтрация** - исключение статических ресурсов и health checks

### Дополнительное логирование (отладка)

Для режима отладки доступно дополнительное логирование тела запросов:

- Включается через `app.debug.enabled=true`
- Логирует тело запросов (до 10KB)
- Включает заголовки
- Настраивается в `RequestLoggingConfig`

## Профили

### Локальная разработка (`local`)

```yaml
logging:
  level:
    azhukov: DEBUG
    azhukov.config.LoggingConfig: DEBUG
    org.springframework.web: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

app:
  debug:
    enabled: true
```

### Продакшн

```yaml
logging:
  level:
    azhukov: INFO
    azhukov.config.LoggingConfig: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

app:
  debug:
    enabled: false
```

## Примеры логов

### Входящий запрос
```
2024-01-15 10:30:45 [http-nio-8080-exec-1] INFO  azhukov.config.LoggingConfig$RequestLoggingFilter - INCOMING REQUEST [a1b2c3d4] GET /api/users | Client: 127.0.0.1 | User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
```

### Исходящий ответ
```
2024-01-15 10:30:45 [http-nio-8080-exec-1] INFO  azhukov.config.LoggingConfig$RequestLoggingFilter - OUTGOING RESPONSE [a1b2c3d4] Status: 200 | Duration: 45ms | Size: 1024 bytes | Content-Type: application/json
```

### Ошибка
```
2024-01-15 10:30:45 [http-nio-8080-exec-1] WARN  azhukov.config.LoggingConfig$RequestLoggingFilter - OUTGOING RESPONSE [a1b2c3d4] Status: 404 | Duration: 12ms | Size: 0 bytes
```

## Исключения из логирования

Следующие запросы не логируются:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/static/*`
- `/favicon.ico`
- `/swagger-ui/*`
- `/v3/api-docs`

## Настройка уровней логирования

### Для включения подробного логирования SQL запросов:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Для логирования Spring Security:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
```

## Файлы логов

- **Основной лог**: `logs/hr-recruiter.log`
- **Локальная разработка**: `logs/hr-recruiter-local.log`
- **Максимальный размер**: 10MB
- **История**: 30 дней (7 дней для локальной разработки)

## Мониторинг производительности

Логирование включает время выполнения запросов, что позволяет:

1. Выявлять медленные запросы
2. Мониторить производительность API
3. Анализировать паттерны использования
4. Оптимизировать критические пути

## Безопасность

- Пароли и чувствительные данные не логируются
- IP адреса логируются для аудита
- User-Agent логируется для анализа клиентов
- Тело запросов логируется только в режиме отладки 