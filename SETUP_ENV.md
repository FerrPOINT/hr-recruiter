# Настройка переменных окружения

## Обзор изменений

Все API ключи OpenRouter и Anthropic теперь используют системные переменные окружения вместо хардкода в конфигурационных файлах.

## Переменные окружения

### Обязательные переменные

```bash
# OpenRouter API Key (основной AI сервис)
OPENROUTER_API_KEY=sk-or-v1-your-openrouter-api-key-here

# Anthropic API Key (альтернативный AI сервис)
ANTHROPIC_API_KEY=sk-ant-your-anthropic-api-key-here
```

### Как получить API ключи

1. **OpenRouter API Key**:
   - Зарегистрируйтесь на https://openrouter.ai/
   - Перейдите в раздел API Keys
   - Создайте новый ключ
   - Скопируйте ключ (начинается с `sk-or-v1-`)

2. **Anthropic API Key**:
   - Зарегистрируйтесь на https://console.anthropic.com/
   - Перейдите в раздел API Keys
   - Создайте новый ключ
   - Скопируйте ключ (начинается с `sk-ant-`)

## Способы установки переменных

### 1. Файл .env (рекомендуется для разработки)

Создайте файл `.env` в корне проекта:

```bash
# Скопируйте env.example
cp env.example .env

# Отредактируйте .env файл
nano .env
```

### 2. Системные переменные окружения

#### Windows (PowerShell)
```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-your-key-here"
$env:ANTHROPIC_API_KEY="sk-ant-your-key-here"
```

#### Windows (Command Prompt)
```cmd
set OPENROUTER_API_KEY=sk-or-v1-your-key-here
set ANTHROPIC_API_KEY=sk-ant-your-key-here
```

#### Linux/macOS
```bash
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
export ANTHROPIC_API_KEY="sk-ant-your-key-here"
```

### 3. Docker Compose

При запуске с Docker Compose переменные автоматически подхватываются из системного окружения:

```bash
# Установите переменные
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
export ANTHROPIC_API_KEY="sk-ant-your-key-here"

# Запустите приложение
docker-compose up -d
```

### 4. IDE (IntelliJ IDEA)

1. Откройте Run/Debug Configurations
2. Выберите вашу конфигурацию
3. В разделе Environment variables добавьте:
   - `OPENROUTER_API_KEY=sk-or-v1-your-key-here`
   - `ANTHROPIC_API_KEY=sk-ant-your-key-here`

## Проверка настройки

### 1. Проверка переменных окружения

```bash
# Windows
echo $env:OPENROUTER_API_KEY
echo $env:ANTHROPIC_API_KEY

# Linux/macOS
echo $OPENROUTER_API_KEY
echo $ANTHROPIC_API_KEY
```

### 2. Проверка в приложении

После запуска приложения проверьте логи:

```bash
# Локальный запуск
./gradlew bootRun

# Docker
docker-compose logs -f app
```

Ищите сообщения:
- `OpenRouter API key configured` - ключ OpenRouter настроен
- `Anthropic API key configured` - ключ Anthropic настроен
- `WARNING: API key not configured` - ключ не настроен

### 3. Тестирование API

```bash
# Проверка доступности AI
curl -X GET http://localhost:8080/api/v1/ai/health

# Тест генерации текста
curl -X POST http://localhost:8080/api/v1/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Привет, как дела?"}'
```

## Fallback значения

Если переменные окружения не установлены, используются fallback значения:

- **OpenRouter**: `sk-or-demo-key` (не работает с реальными запросами)
- **Anthropic**: `sk-demo-key` (не работает с реальными запросами)

## Безопасность

⚠️ **Важно**: Никогда не коммитьте реальные API ключи в репозиторий!

- Файл `.env` добавлен в `.gitignore`
- В конфигурационных файлах используются только fallback значения
- Реальные ключи должны передаваться через переменные окружения

## Troubleshooting

### Проблема: "API key not configured"

**Решение**: Убедитесь, что переменные окружения установлены:

```bash
# Проверьте переменные
echo $OPENROUTER_API_KEY
echo $ANTHROPIC_API_KEY

# Если пустые, установите их
export OPENROUTER_API_KEY="your-key-here"
```

### Проблема: "Invalid API key"

**Решение**: Проверьте правильность ключа:

- OpenRouter ключи начинаются с `sk-or-v1-`
- Anthropic ключи начинаются с `sk-ant-`
- Убедитесь, что ключ не содержит лишних символов

### Проблема: "Rate limit exceeded"

**Решение**: 
- Проверьте лимиты на сайте провайдера
- Увеличьте интервалы между запросами
- Рассмотрите возможность обновления тарифного плана 