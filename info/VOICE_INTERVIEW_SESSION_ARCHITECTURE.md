# Архитектура параллельных голосовых интервью с индивидуальными вопросами (ElevenLabs + Backend)

## 1. Общая идея

Каждый кандидат проходит интервью по своей вакансии, получает индивидуальный набор вопросов. Несколько кандидатов могут проходить интервью одновременно, и каждый получает только свои вопросы, связанные с конкретной вакансией.

---

## 2. Основные сущности

- **Vacancy (Вакансия)**
  - id
  - title
  - ...
- **Question (Вопрос)**
  - id
  - vacancy_id (внешний ключ на Vacancy)
  - text
  - order (порядок)
- **Candidate (Кандидат)**
  - id
  - name, email, ...
- **InterviewSession (Сессия интервью)**
  - id (sessionId, уникальный для каждой сессии)
  - candidate_id
  - vacancy_id
  - started_at, finished_at
  - ...
- **InterviewProgress (Прогресс по вопросам)**
  - session_id
  - question_id
  - answered (bool)
  - answer_text
  - answer_audio_url
  - duration_ms
  - confidence
  - timestamp

---

## 3. Поток взаимодействия (Sequence)

1. **HR создает вакансию и сценарий вопросов**
2. **Кандидату выдается уникальная ссылка на интервью**
   - Пример: `/interview/SESSION_ID`
3. **Кандидат заходит на страницу, фронт инициализирует сессию**
   - Создается запись InterviewSession
4. **Агент ElevenLabs (через SDK/Webhook) запрашивает следующий вопрос:**
   - GET `/api/interview/{sessionId}/next`
5. **Backend по sessionId:**
   - Определяет вакансию
   - Находит следующий неотвеченный вопрос
   - Возвращает текст вопроса
6. **Агент озвучивает вопрос, кандидат отвечает**
7. **Ответ отправляется на backend:**
   - POST `/api/interview/{sessionId}/answer` (с текстом, аудио, метриками)
   - Backend сохраняет ответ, duration, confidence и т.д. в InterviewProgress
8. **Повтор шагов 4–7 до конца сценария**
9. **После последнего вопроса агент говорит финальную реплику**
10. **Сессия завершается, результаты доступны HR**

---

## 4. Пример API

### Получить следующий вопрос
```http
GET /api/interview/{sessionId}/next
```
**Ответ:**
```json
{
  "questionId": 5,
  "text": "Расскажите о вашем опыте работы в команде"
}
```

### Сохранить ответ
```http
POST /api/interview/{sessionId}/answer
Content-Type: application/json

{
  "questionId": 5,
  "answerText": "Я работал в команде из 5 человек...",
  "audioUrl": "https://.../audio.mp3",
  "durationMs": 12345,
  "confidence": 0.92
}
```

### Завершить сессию
```http
POST /api/interview/{sessionId}/end
```

---

## 5. Логика backend (Spring)

- **/next**: По sessionId находит вакансию, ищет следующий неотвеченный вопрос, возвращает его.
- **/answer**: Сохраняет ответ, duration, confidence, аудио.
- **/end**: Помечает сессию завершённой.
- Все операции изолированы по sessionId — никакой пересекающейся логики между сессиями.

---

## 6. Масштабируемость и параллельность

- Сессии полностью независимы друг от друга (sessionId — ключ).
- Вопросы для каждой вакансии индивидуальны, backend возвращает только нужные вопросы для конкретной сессии.
- Можно запускать любое количество интервью одновременно.
- Нет race condition: прогресс хранится в БД, а не в памяти.

---

## 7. Пример структуры таблиц (SQL)

```sql
CREATE TABLE vacancy (
  id BIGSERIAL PRIMARY KEY,
  title TEXT
);

CREATE TABLE question (
  id BIGSERIAL PRIMARY KEY,
  vacancy_id BIGINT REFERENCES vacancy(id),
  text TEXT,
  "order" INT
);

CREATE TABLE candidate (
  id BIGSERIAL PRIMARY KEY,
  name TEXT,
  email TEXT
);

CREATE TABLE interview_session (
  id UUID PRIMARY KEY,
  candidate_id BIGINT REFERENCES candidate(id),
  vacancy_id BIGINT REFERENCES vacancy(id),
  started_at TIMESTAMP,
  finished_at TIMESTAMP
);

CREATE TABLE interview_progress (
  session_id UUID REFERENCES interview_session(id),
  question_id BIGINT REFERENCES question(id),
  answered BOOLEAN,
  answer_text TEXT,
  answer_audio_url TEXT,
  duration_ms INT,
  confidence FLOAT,
  timestamp TIMESTAMP,
  PRIMARY KEY (session_id, question_id)
);
```

---

## 8. ElevenLabs интеграция

- В настройках агента указываешь ClientTool/Webhook:
  - URL: `https://your-backend/api/interview/{sessionId}/next`
- Агент сам подставляет sessionId, backend возвращает нужный вопрос.
- Ответы отправляются на backend через POST `/answer`.

---

## 9. Преимущества

- Любое количество параллельных интервью
- Индивидуальные вопросы для каждой вакансии
- Надёжное хранение прогресса и ответов
- Масштабируемость и простота поддержки

---

## 10. Варианты расширения

- Ветвление сценариев (разные вопросы в зависимости от ответов)
- Аналитика по сессиям и вакансиям
- Интеграция с внешними CRM/HRM
- Автоматическая оценка ответов через AI 