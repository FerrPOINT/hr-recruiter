# Руководство по авторизации и использованию токенов в HR Recruiter API

## 1. Виды авторизации

### 1.1. Авторизация администратора (CRM)
- **Endpoint:** `POST /auth/login`
- **Request:**
  ```json
  {
    "email": "admin@example.com",
    "password": "your_password"
  }
  ```
- **Response:**
  ```json
  {
    "token": "<JWT_TOKEN>",
    "user": { ... }
  }
  ```
- **Использование:**
  - Все методы для управления вакансиями, пользователями, аналитикой и т.д. требуют заголовок:
    ```http
    Authorization: Bearer <JWT_TOKEN>
    ```

### 1.2. Авторизация кандидата
- **Endpoint:** `POST /candidates/auth`
- **Request:**
  ```json
  {
    "firstName": "Иван",
    "lastName": "Иванов",
    "email": "ivanov@example.com",
    "phone": "+79991234567",
    "positionId": 123
  }
  ```
- **Response:**
  ```json
  {
    "token": "<CANDIDATE_JWT>",
    "candidate": { ... }
  }
  ```
- **Использование:**
  - Все методы для кандидата (например, прохождение интервью, отправка ответов) требуют заголовок:
    ```http
    Authorization: Bearer <CANDIDATE_JWT>
    ```

## 2. Права доступа по ролям

### 2.1. Роль ADMIN
**Полный доступ к системе:**
- ✅ Управление вакансиями (`/positions`)
- ✅ Управление пользователями (`/users`)
- ✅ Управление интервью (`/interviews`)
- ✅ AI функциональность (`/ai`)
- ✅ Управление вопросами (`/questions`)
- ✅ Настройки системы (`/settings`)
- ✅ Прохождение интервью (`/api/interviews`)
- ✅ Управление аккаунтом (`/account`)
- ✅ Аналитика и отчеты (`/stats/*`, `/reports`)
- ✅ Архив и обучающие материалы (`/archive`, `/learn`)

### 2.2. Роль CANDIDATE
**Ограниченный доступ:**
- ❌ Управление вакансиями
- ❌ Управление пользователями
- ❌ AI функциональность
- ❌ Управление вопросами
- ❌ Настройки системы
- ✅ Прохождение интервью (`/api/interviews`)
- ✅ Управление аккаунтом (`/account`)
- ✅ Кандидатские функции (`/candidates`)

### 2.3. Смешанные эндпоинты
Некоторые эндпоинты доступны обеим ролям:
- `/interviews/{id}` - просмотр интервью (ADMIN + CANDIDATE)
- `/api/interviews/*` - голосовые интервью (ADMIN + CANDIDATE)
- `/account/*` - управление аккаунтом (ADMIN + CANDIDATE)

## 3. Использование токенов

- **JWT-токен** должен передаваться в каждом защищённом запросе через заголовок:
  ```http
  Authorization: Bearer <TOKEN>
  ```
- **Токены для админов и кандидатов несовместимы для большинства эндпоинтов!**
  - Токен администратора не даёт доступ к кандидатским эндпоинтам и наоборот.
  - Исключение: смешанные эндпоинты доступны обеим ролям.

## 4. Примеры вызова защищённых методов

### 4.1. Получить список вакансий (только админ)
```http
GET /positions
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### 4.2. Пройти голосовое интервью (админ или кандидат)
```http
POST /api/interviews/{interviewId}/answer
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "questionId": 456,
  "text": "Мой ответ на вопрос..."
}
```

### 4.3. Создать интервью (только админ)
```http
POST /interviews?candidateId=123
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### 4.4. Получить свой аккаунт (любой авторизованный)
```http
GET /account
Authorization: Bearer <JWT_TOKEN>
```

### 4.5. Получить статистику кандидатов (только админ)
```http
GET /stats/candidates
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### 4.6. Получить месячные отчеты (только админ)
```http
GET /reports
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### 4.7. Получить обучающие материалы (только админ)
```http
GET /learn
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

## 5. Best Practices
- **Не храните токены в localStorage** — используйте httpOnly cookies или безопасное хранилище.
- **Обновляйте токен по истечении срока действия** (реализуйте refresh flow, если требуется).
- **Проверяйте права доступа на бэкенде** — не доверяйте только фронтенду.
- **Для интеграции с внешними сервисами** используйте отдельные сервисные аккаунты и токены.

## 6. Ошибки авторизации
- `401 Unauthorized` — неверный или отсутствующий токен.
- `403 Forbidden` — недостаточно прав для доступа к ресурсу.

---

# Инструкция для фронтенда: интеграция авторизации

## 7. Схема работы авторизации (Frontend Auth Flow)

1. **Логин:**
   - Админ: POST `/auth/login` с email и паролем.
   - Кандидат: POST `/candidates/auth` с анкетой и positionId.
   - В ответе приходит JWT-токен и объект пользователя/кандидата.
2. **Хранение токена:**
   - Рекомендуется хранить токен в httpOnly cookie (если сервер поддерживает) или в памяти приложения (например, в state менеджере).
   - Не храните токен в localStorage/sessionStorage без крайней необходимости.
3. **Использование токена:**
   - Для всех защищённых запросов добавляйте заголовок:
     ```js
     fetch('/positions', {
       headers: { 'Authorization': 'Bearer ' + token }
     })
     ```
4. **Обработка ошибок:**
   - 401 — перенаправить на страницу логина.
   - 403 — показать сообщение "Недостаточно прав".
5. **Logout:**
   - Просто удалите токен из хранилища и сбросьте состояние пользователя.

## 8. Примеры запросов на фронте (JS/TypeScript)

### 8.1. Логин админа
```js
const res = await fetch('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});
const data = await res.json();
if (res.ok) {
  setToken(data.token); // сохранить токен
} else {
  alert('Ошибка авторизации');
}
```

### 8.2. Логин кандидата
```js
const res = await fetch('/candidates/auth', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ firstName, lastName, email, phone, positionId })
});
const data = await res.json();
if (res.ok) {
  setToken(data.token);
} else {
  alert('Ошибка авторизации кандидата');
}
```

### 8.3. Запрос с токеном
```js
const res = await fetch('/positions', {
  headers: { 'Authorization': 'Bearer ' + token }
});
```

### 8.4. Обработка ошибок 401/403
```js
if (res.status === 401) {
  // Перенаправить на логин
}
if (res.status === 403) {
  // Показать "Нет доступа"
}
```

## 9. Как определить роль пользователя на фронте

- JWT-токен содержит payload с ролью пользователя ("role": "ADMIN" или "CANDIDATE").
- Можно декодировать payload на фронте (например, через atob):

```js
function parseJwt(token) {
  return JSON.parse(atob(token.split('.')[1]));
}
const payload = parseJwt(token);
console.log(payload.role); // 'ADMIN' или 'CANDIDATE'
```

- Не используйте это для защиты маршрутов — только для UI/UX! Защита всегда на бэкенде.

## 10. FAQ для фронта

- **Как понять, что токен истёк?**
  - Сервер вернёт 401. Можно также смотреть на поле `exp` в payload JWT (время истечения).
- **Как отличить кандидата от админа?**
  - По полю `role` в payload токена.
- **Как правильно делать logout?**
  - Удалить токен из хранилища, сбросить состояние пользователя, перенаправить на логин.
- **Что делать при 403?**
  - Показать сообщение "Недостаточно прав" и скрыть недоступные разделы UI.
- **Может ли админ проходить интервью?**
  - Да, админ имеет доступ ко всем функциям, включая прохождение интервью.

---

Документ поддерживается автоматически. Все примеры соответствуют OpenAPI-спецификации проекта. 