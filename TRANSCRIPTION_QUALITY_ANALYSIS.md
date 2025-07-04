# 🎤 Анализ качества транскрибации

## 📋 Проблема

При тестировании микрофона текст распознается лучше, чем при ответах на вопросы интервью.

## 🔍 Анализ различий

### Тестирование микрофона (`/ai/transcribe`)
- **Endpoint**: `POST /api/v1/ai/transcribe`
- **Обработка**: Только Whisper ASR
- **Результат**: Сырой транскрибированный текст
- **Форматирование**: Нет

### Ответы на вопросы (`/ai/transcribe-answer`)
- **Endpoint**: `POST /api/v1/ai/transcribe-answer`
- **Обработка**: Whisper ASR → Claude AI → База данных
- **Результат**: Отформатированный текст
- **Форматирование**: Claude AI

## 🎯 Корень проблемы

### 1. Claude форматирование
Claude может искажать русские слова при "исправлении" транскрибации:
- Заменяет русские слова на английские
- "Исправляет" русские названия и термины
- Переводит русские слова на английский

### 2. Разные параметры Whisper
Хотя используются одинаковые методы, могут быть различия в:
- Контексте обработки
- Параметрах модели
- Настройках качества

## 🔧 Выполненные исправления

### 1. Улучшен промпт для Claude
```java
КРИТИЧЕСКИ ВАЖНЫЕ ПРАВИЛА:
- НИКОГДА не заменяй русские слова на английские
- НИКОГДА не "исправляй" русские названия, имена, термины
- НИКОГДА не переводишь русские слова на английский
- Сохраняй ВСЕ русские слова точно как они есть
- Если слово звучит как русское - оставляй его русским
- Если слово звучит как английское - оставляй его английским
- НЕ угадывай и НЕ предполагай правильное написание
```

### 2. Улучшены параметры Whisper
```yaml
app:
  ai:
    transcription:
      whisper:
        model: medium # Более качественная модель
        language: ru # Явно указываем русский язык
        word-timestamps: true # Временные метки слов
        condition-on-previous-text: false # Без контекста
        temperature: 0.0 # Нулевая температура для точности
        timeout: 60000 # Увеличенный timeout
```

### 3. Создан инструмент сравнения
Файл `test-transcription-comparison.html` позволяет:
- Записать один и тот же текст
- Сравнить результаты Whisper vs Whisper+Claude
- Выявить различия в качестве

## 🧪 Тестирование

### Использование инструмента сравнения:
1. Откройте `test-transcription-comparison.html`
2. Запишите тестовый текст на русском языке
3. Нажмите "Сравнить оба" для получения результатов
4. Сравните качество между методами

### Примеры тестовых фраз:
- "Я работал с Java и Spring Boot"
- "Использовал React для фронтенда"
- "Делал API на Node.js"
- "База данных PostgreSQL"

## 📊 Мониторинг

### Логи для анализа:
```log
# Whisper транскрибация
INFO - Whisper transcription completed in 1500 ms, length: 45 chars

# Claude форматирование
INFO - Claude formatting completed in 300 ms, length: 42 chars

# Полный пайплайн
INFO - Total transcription pipeline completed successfully in 1850 ms
```

### Метрики качества:
- Длина текста до/после форматирования
- Время обработки каждого этапа
- Количество изменений в тексте

## 🎯 Рекомендации

### 1. Для разработки:
- Используйте `test-transcription-comparison.html` для тестирования
- Сравнивайте сырой и отформатированный текст
- Логируйте различия для анализа

### 2. Для продакшена:
- Мониторьте качество форматирования
- Настройте алерты при значительных изменениях
- Регулярно обновляйте промпт Claude

### 3. Для пользователей:
- Объясните разницу между тестированием и реальными ответами
- Предоставьте возможность получить сырой текст
- Добавьте обратную связь о качестве

## 🔄 Дальнейшие улучшения

### 1. Адаптивное форматирование
- Определять язык текста автоматически
- Применять разные правила для русского/английского
- Сохранять оригинальные термины

### 2. Пользовательские настройки
- Возможность отключить Claude форматирование
- Выбор уровня форматирования
- Персональные правила для терминов

### 3. Машинное обучение
- Обучение на пользовательских данных
- Улучшение качества распознавания
- Персонализация под пользователя

## 📝 Заключение

Проблема решена путем:
1. Улучшения промпта Claude для сохранения русских слов
2. Оптимизации параметров Whisper
3. Создания инструмента для сравнения качества
4. Добавления мониторинга и логирования

Теперь качество транскрибации должно быть одинаковым для тестирования и реальных ответов. 