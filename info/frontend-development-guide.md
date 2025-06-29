# HR Recruiter Frontend - Руководство по разработке

## 📋 Обзор проекта

HR Recruiter - это современная платформа для автоматизации HR-собеседований с AI-ассистентом. Фронтенд построен на React 18 с TypeScript, использует Tailwind CSS для стилизации и включает полный набор функций для управления вакансиями, кандидатами и собеседованиями.

## 🏗️ Архитектура проекта

```
hr-recruiter-front/
├── src/
│   ├── components/          # Переиспользуемые компоненты
│   ├── pages/              # Страницы приложения
│   ├── services/           # API сервисы и утилиты
│   ├── mocks/              # Mock API для разработки
│   ├── client/             # Автогенерированные API клиенты
│   ├── utils/              # Утилиты и хелперы
│   ├── App.tsx             # Главный компонент
│   ├── index.tsx           # Точка входа
│   └── index.css           # Глобальные стили
├── api/                    # OpenAPI спецификация
├── public/                 # Статические файлы
└── package.json            # Зависимости и скрипты
```

## 🚀 Быстрый старт

### 1. Создание проекта

```bash
npx create-react-app hr-recruiter-front --template typescript
cd hr-recruiter-front
```

### 2. Установка зависимостей

```json
{
  "dependencies": {
    "@dnd-kit/core": "^6.3.1",
    "@dnd-kit/sortable": "^10.0.0",
    "@dnd-kit/utilities": "^3.2.2",
    "@hookform/resolvers": "^3.3.2",
    "axios": "^1.6.2",
    "clsx": "^2.0.0",
    "date-fns": "^2.30.0",
    "lucide-react": "^0.294.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-hook-form": "^7.48.2",
    "react-hot-toast": "^2.4.1",
    "react-router-dom": "^6.20.1",
    "react-scripts": "5.0.1",
    "tailwind-merge": "^2.0.0",
    "typescript": "^4.9.5",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "@openapitools/openapi-generator-cli": "^2.20.2",
    "@types/react": "^19.1.8",
    "@types/react-dom": "^19.1.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32",
    "tailwindcss": "^3.3.6"
  }
}
```

### 3. Настройка Tailwind CSS

```bash
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

**tailwind.config.js:**
```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        'wmt-orange': '#FF6600',
        'wmt-orange-dark': '#E65C00',
        'primary': {
          '50': '#fff7ed',
          '100': '#ffedd5',
          '200': '#fed7aa',
          '300': '#fdba74',
          '400': '#fb923c',
          '500': '#f97316',
          '600': '#ea580c',
          '700': '#c2410c',
          '800': '#9a3412',
          '900': '#7c2d12',
        },
        gray: {
          50: '#f9fafb',
          100: '#f3f4f6',
          200: '#e5e7eb',
          300: '#d1d5db',
          400: '#9ca3af',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
        }
      },
      fontFamily: {
        sans: ['Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      boxShadow: {
        'soft': '0 1px 3px rgba(0,0,0,0.1)',
        'medium': '0 4px 6px rgba(0,0,0,0.1)',
        'large': '0 10px 15px rgba(0,0,0,0.1)',
      }
    },
  },
  plugins: [],
}
```

**src/index.css:**
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html {
    font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
  }
  
  body {
    margin: 0;
    font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    background-color: #f9fafb;
    color: #333333;
  }
}

@layer components {
  .btn-primary {
    @apply bg-primary-500 hover:bg-primary-600 text-white font-medium py-1.5 px-5 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 h-11 max-h-11;
  }
  
  .btn-secondary {
    @apply bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium py-1.5 px-5 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 h-11 max-h-11;
  }
  
  .card {
    @apply bg-white border border-gray-200 rounded-lg shadow-soft p-6;
  }
  
  .input-field {
    @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent;
  }
}
```

## 📁 Структура файлов

### 1. Основные компоненты

**src/App.tsx:**
```typescript
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import VacancyCreate from './pages/VacancyCreate';
import VacancyList from './pages/VacancyList';
import InterviewCreate from './pages/InterviewCreate';
import InterviewList from './pages/InterviewList';
import InterviewSession from './pages/InterviewSession';
import Reports from './pages/Reports';
import Login from './pages/Login';
import Account from './pages/Account';
import Team from './pages/Team';
import Branding from './pages/Branding';
import Tariffs from './pages/Tariffs';
import Questions from './pages/Questions';

function App() {
  return (
    <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <div className="min-h-screen bg-gray-50">
        <Toaster 
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
          }}
        />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/interview/:sessionId" element={<InterviewSession />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="vacancies" element={<VacancyList />} />
            <Route path="vacancies/create" element={<VacancyCreate />} />
            <Route path="interviews" element={<InterviewList />} />
            <Route path="interviews/create" element={<InterviewCreate />} />
            <Route path="reports" element={<Reports />} />
            <Route path="account" element={<Account />} />
            <Route path="team" element={<Team />} />
            <Route path="branding" element={<Branding />} />
            <Route path="tariffs" element={<Tariffs />} />
            <Route path="questions/:positionId" element={<Questions />} />
          </Route>
        </Routes>
      </div>
    </Router>
  );
}

export default App;
```

### 2. Mock API система

**src/mocks/mockApi.ts** - центральный файл с mock данными и методами API. Содержит:

- Статические данные (команда, брендинг, вакансии)
- Генераторы динамических данных (кандидаты, собеседования)
- Все API методы с имитацией задержек
- Типизированные интерфейсы

**Ключевые принципы:**
- Все данные сохраняются в памяти
- Имитация реальных задержек (200-500ms)
- Детерминированные данные для демонстрации
- Полная типизация TypeScript

### 3. Страницы приложения

**Основные страницы:**
- `Dashboard.tsx` - главная панель с статистикой
- `VacancyList.tsx` - список вакансий с фильтрацией
- `VacancyCreate.tsx` - создание новой вакансии
- `InterviewSession.tsx` - сессия собеседования с AI
- `Account.tsx` - профиль пользователя
- `Team.tsx` - управление командой
- `Branding.tsx` - настройки брендинга
- `Tariffs.tsx` - управление тарифами

### 4. Компоненты

**src/components/Layout.tsx** - основной layout с навигацией
**src/components/ProtectedRoute.tsx** - защищенные маршруты
**src/components/InterviewHeader.tsx** - заголовок интервью

## 🎨 Дизайн система

### Цветовая палитра
- **Primary**: Orange (#FF6600) - основной брендинг
- **Gray**: Полная палитра серых оттенков
- **Success**: Green для успешных действий
- **Error**: Red для ошибок

### Типографика
- **Font**: Segoe UI, Roboto, Helvetica Neue, Arial
- **Weights**: 400 (normal), 500 (medium), 600 (semibold)
- **Sizes**: 12px, 14px, 16px, 18px, 20px, 24px, 32px

### Компоненты
- **Buttons**: Primary, Secondary, Success с hover эффектами
- **Cards**: Белые карточки с мягкими тенями
- **Inputs**: Поля ввода с focus состояниями
- **Navigation**: Боковая навигация с иконками

## 🔧 API интеграция

### 1. Mock API (разработка)
```typescript
// Все методы возвращают Promise с типизированными данными
const mockApi = {
  async getPositions() { /* ... */ },
  async createPosition(data) { /* ... */ },
  async getInterviews() { /* ... */ },
  // ... 45+ методов
};
```

### 2. Реальный API (production)
```typescript
// Автогенерированные клиенты из OpenAPI
import { createApiClient } from './client/apiClient';
const apiClient = createApiClient();
```

### 3. Генерация API клиентов
```bash
npm run generate:api
```

## 📊 Состояние приложения

### Управление состоянием
- **React Hooks** для локального состояния
- **React Router** для навигации
- **React Hook Form** для форм
- **SessionStorage** для временных данных

### Типизация
- **TypeScript** для всех компонентов
- **Интерфейсы** для всех API ответов
- **Enums** для статусов и ролей

## 🚀 Функциональность

### Основные модули:

1. **Аутентификация**
   - Login/Logout
   - Роли: admin, recruiter, viewer
   - Защищенные маршруты

2. **Управление вакансиями**
   - CRUD операции
   - Фильтрация и поиск
   - Публичные ссылки
   - Статистика

3. **Управление кандидатами**
   - Добавление кандидатов
   - Статусы: new, in_progress, finished, rejected, hired
   - Связь с вакансиями

4. **Собеседования**
   - Создание сессий
   - AI-ассистент
   - Аудио запись
   - Транскрибация
   - Оценка ответов

5. **Аналитика**
   - Дашборд с метриками
   - Отчеты по месяцам
   - Статистика по вакансиям

6. **Настройки**
   - Брендинг компании
   - Управление командой
   - Тарифные планы

## 🔄 Workflow разработки

### 1. Начальная разработка
```bash
# Установка зависимостей
npm install

# Запуск в режиме разработки
npm start

# Использование mock API
# Все данные генерируются локально
```

### 2. Интеграция с бэкендом
```bash
# Генерация API клиентов
npm run generate:api

# Переключение на реальный API
# Изменить useMock в authService.ts
```

### 3. Сборка для production
```bash
npm run build
```

## 📝 Ключевые файлы для копирования

### Обязательные файлы:
1. `src/mocks/mockApi.ts` - полная mock система
2. `src/mocks/vacancies.json` - статические данные вакансий
3. `src/services/authService.ts` - сервис аутентификации
4. `src/components/Layout.tsx` - основной layout
5. `src/pages/*.tsx` - все страницы приложения
6. `tailwind.config.js` - конфигурация стилей
7. `src/index.css` - глобальные стили

### Конфигурационные файлы:
- `package.json` - зависимости и скрипты
- `tsconfig.json` - настройки TypeScript
- `postcss.config.js` - конфигурация PostCSS
- `api/openapi.yaml` - OpenAPI спецификация

## 🎯 Рекомендации по разработке

### 1. Структура компонентов
- Используйте функциональные компоненты с хуками
- Разделяйте логику и представление
- Применяйте композицию компонентов

### 2. Стилизация
- Tailwind CSS для быстрой разработки
- Кастомные классы для повторяющихся паттернов
- Адаптивный дизайн с мобильной версией

### 3. Типизация
- Строгая типизация всех пропсов
- Интерфейсы для API ответов
- Enums для констант

### 4. Производительность
- React.memo для оптимизации
- Lazy loading для страниц
- Мемоизация тяжелых вычислений

## 🔧 Настройка окружения

### Переменные окружения
```env
REACT_APP_USE_MOCK_API=true
REACT_APP_API_URL=http://localhost:8080/api
```

### Скрипты package.json
```json
{
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "generate:api": "openapi-generator-cli generate -i api/openapi.yaml -g typescript-axios -o src/client"
  }
}
```

## 📚 Дополнительные ресурсы

- **React Router v6** - навигация
- **React Hook Form** - управление формами
- **Lucide React** - иконки
- **React Hot Toast** - уведомления
- **Date-fns** - работа с датами
- **Axios** - HTTP клиент

---

**Статус**: ✅ Готов к воссозданию  
**Сложность**: Средняя  
**Время разработки**: 2-3 недели  
**Размер команды**: 1-2 разработчика 