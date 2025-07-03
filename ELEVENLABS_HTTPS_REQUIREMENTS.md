# 🔐 HTTPS и CSP требования для ElevenLabs интеграции

## 📋 Обзор

Интеграция ElevenLabs требует дополнительных настроек безопасности помимо базовых требований для микрофона. Этот документ описывает все необходимые изменения в конфигурации.

## 🔐 HTTPS требования

### 1. Обязательность HTTPS

#### Для микрофона (текущее)
- ✅ **Локально**: HTTP работает на localhost
- ❌ **Продакшн**: HTTPS обязателен
- ✅ **Подготовлено**: Скрипты для SSL настройки

#### Для ElevenLabs (новое)
- ✅ **API вызовы**: HTTPS для ElevenLabs API (api.elevenlabs.io)
- ✅ **Аудио поток**: HTTPS для передачи аудио данных
- ✅ **Безопасность**: Шифрование всех данных в транзите
- ✅ **Сертификаты**: Валидные SSL сертификаты для домена

### 2. Дополнительные домены

#### ElevenLabs API домены
```
https://api.elevenlabs.io - Основной API
https://elevenlabs.io - Документация и ресурсы
https://*.elevenlabs.io - Поддомены (если используются)
```

#### Текущие домены
```
https://your-domain.com - Основной сайт
https://your-api-host:8080 - Backend API
```

## 🛡️ CSP (Content Security Policy) требования

### 1. Текущая конфигурация CSP

```nginx
add_header Content-Security-Policy "
    default-src 'self';
    script-src 'self' 'unsafe-inline' 'unsafe-eval';
    style-src 'self' 'unsafe-inline';
    img-src 'self' data: https:;
    media-src 'self' blob:;
    connect-src 'self' https://your-api-host:8080;
    frame-ancestors 'none';
" always;
```

### 2. Обновленная конфигурация CSP для ElevenLabs

```nginx
add_header Content-Security-Policy "
    default-src 'self';
    script-src 'self' 'unsafe-inline' 'unsafe-eval';
    style-src 'self' 'unsafe-inline';
    img-src 'self' data: https:;
    media-src 'self' blob: https://api.elevenlabs.io https://elevenlabs.io;
    connect-src 'self' 
        https://your-api-host:8080 
        https://api.elevenlabs.io 
        https://elevenlabs.io;
    frame-ancestors 'none';
    worker-src 'self' blob:;
    child-src 'self' blob:;
" always;
```

### 3. Детальное объяснение директив

#### media-src
```
media-src 'self' blob: https://api.elevenlabs.io https://elevenlabs.io;
```
- **'self'**: Разрешает медиа с того же домена
- **blob:** : Разрешает blob URL для аудио файлов
- **https://api.elevenlabs.io**: ElevenLabs API для аудио
- **https://elevenlabs.io**: Основной домен ElevenLabs

#### connect-src
```
connect-src 'self' 
    https://your-api-host:8080 
    https://api.elevenlabs.io 
    https://elevenlabs.io;
```
- **'self'**: Разрешает соединения с тем же доменом
- **https://your-api-host:8080**: Ваш backend API
- **https://api.elevenlabs.io**: ElevenLabs API вызовы
- **https://elevenlabs.io**: Дополнительные ресурсы ElevenLabs

#### worker-src и child-src
```
worker-src 'self' blob:;
child-src 'self' blob:;
```
- **'self'**: Разрешает Web Workers с того же домена
- **blob:** : Разрешает blob URL для Web Workers (аудио обработка)

## 🔧 Обновление конфигурации

### 1. Nginx конфигурация

#### Обновленный скрипт setup-ssl.sh
```bash
# Обновляем конфигурацию с CSP заголовками для ElevenLabs
sudo tee /etc/nginx/sites-available/$DOMAIN << EOF
# Редирект с HTTP на HTTPS
server {
    listen 80;
    server_name $DOMAIN www.$DOMAIN;
    return 301 https://\$server_name\$request_uri;
}

# Основной HTTPS сервер
server {
    listen 443 ssl http2;
    server_name $DOMAIN www.$DOMAIN;
    
    # SSL сертификаты (автоматически настроены certbot)
    ssl_certificate /etc/letsencrypt/live/$DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;
    
    # CSP заголовки для микрофона и ElevenLabs
    add_header Content-Security-Policy "
        default-src 'self';
        script-src 'self' 'unsafe-inline' 'unsafe-eval';
        style-src 'self' 'unsafe-inline';
        img-src 'self' data: https:;
        media-src 'self' blob: https://api.elevenlabs.io https://elevenlabs.io;
        connect-src 'self' 
            https://your-api-host:8080 
            https://api.elevenlabs.io 
            https://elevenlabs.io;
        frame-ancestors 'none';
        worker-src 'self' blob:;
        child-src 'self' blob:;
    " always;
    
    root /var/www/$DOMAIN/build;
    index index.html;
    
    location / {
        try_files \$uri \$uri/ /index.html;
    }
    
    # Кэширование статических файлов
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF
```

### 2. Apache конфигурация

```apache
<VirtualHost *:443>
    ServerName your-domain.com
    DocumentRoot /path/to/build
    
    SSLEngine on
    SSLCertificateFile /path/to/certificate.crt
    SSLCertificateKeyFile /path/to/private.key
    
    # CSP для микрофона и ElevenLabs
    Header always set Content-Security-Policy "
        default-src 'self';
        script-src 'self' 'unsafe-inline' 'unsafe-eval';
        style-src 'self' 'unsafe-inline';
        img-src 'self' data: https:;
        media-src 'self' blob: https://api.elevenlabs.io https://elevenlabs.io;
        connect-src 'self' 
            https://your-api-host:8080 
            https://api.elevenlabs.io 
            https://elevenlabs.io;
        frame-ancestors 'none';
        worker-src 'self' blob:;
        child-src 'self' blob:;
    "
    
    <Directory /path/to/build>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

### 3. Переменные окружения

#### Добавление ElevenLabs API ключа
```bash
# .env файл
REACT_APP_RECRUITER_API_HOST=your-api-host:8080
REACT_APP_ELEVENLABS_API_KEY=your-elevenlabs-api-key
REACT_APP_ELEVENLABS_API_URL=https://api.elevenlabs.io
NODE_ENV=production
```

#### Backend переменные
```bash
# Backend .env
ELEVENLABS_API_KEY=your-elevenlabs-api-key
ELEVENLABS_API_URL=https://api.elevenlabs.io
ELEVENLABS_DEFAULT_VOICE_ID=your-default-voice-id
ELEVENLABS_MODEL_ID=eleven_multilingual_v2
```

## 🧪 Тестирование конфигурации

### 1. Проверка CSP заголовков

```bash
# Проверка CSP заголовков
curl -I https://your-domain.com | grep -i "content-security-policy"

# Ожидаемый результат:
# Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; media-src 'self' blob: https://api.elevenlabs.io https://elevenlabs.io; connect-src 'self' https://your-api-host:8080 https://api.elevenlabs.io https://elevenlabs.io; frame-ancestors 'none'; worker-src 'self' blob:; child-src 'self' blob:;
```

### 2. Тестирование ElevenLabs API

```javascript
// Тест в консоли браузера
fetch('https://api.elevenlabs.io/v1/voices', {
  headers: {
    'xi-api-key': 'your-api-key'
  }
})
.then(response => response.json())
.then(data => console.log('✅ ElevenLabs API доступен:', data))
.catch(error => console.log('❌ Ошибка ElevenLabs API:', error));
```

### 3. Проверка аудио функций

```javascript
// Тест воспроизведения аудио
const audio = new Audio('blob:https://your-domain.com/audio-blob');
audio.play()
  .then(() => console.log('✅ Аудио воспроизведение работает'))
  .catch(error => console.log('❌ Ошибка аудио:', error));
```

## 🔍 Диагностика проблем

### 1. CSP ошибки в консоли

#### Ошибка: "Refused to connect to 'https://api.elevenlabs.io'"
**Решение:** Добавить `https://api.elevenlabs.io` в `connect-src`

#### Ошибка: "Refused to load media from 'https://api.elevenlabs.io'"
**Решение:** Добавить `https://api.elevenlabs.io` в `media-src`

#### Ошибка: "Refused to create a worker from 'blob:'"
**Решение:** Добавить `blob:` в `worker-src`

### 2. HTTPS ошибки

#### Ошибка: "Mixed Content"
**Решение:** Убедиться, что все ресурсы загружаются по HTTPS

#### Ошибка: "SSL Certificate Error"
**Решение:** Проверить валидность SSL сертификатов

### 3. ElevenLabs API ошибки

#### Ошибка: "401 Unauthorized"
**Решение:** Проверить API ключ ElevenLabs

#### Ошибка: "429 Too Many Requests"
**Решение:** Проверить лимиты API и реализовать rate limiting

## 📊 Мониторинг

### 1. Логи для отслеживания

```javascript
// Клиентские логи
console.log('🎵 ElevenLabs TTS:', 'Генерация речи начата');
console.log('🎵 ElevenLabs STT:', 'Транскрибация начата');
console.log('🎵 CSP Check:', 'Проверка CSP заголовков');

// Серверные логи
console.log('🔐 ElevenLabs API:', 'API вызов выполнен');
console.log('🔐 CSP Headers:', 'CSP заголовки отправлены');
console.log('🔐 SSL Check:', 'SSL сертификат валиден');
```

### 2. Метрики для мониторинга

- **CSP violations**: Количество нарушений CSP
- **ElevenLabs API calls**: Количество API вызовов
- **SSL errors**: Ошибки SSL сертификатов
- **Mixed content**: Смешанный контент
- **Audio playback errors**: Ошибки воспроизведения

## ✅ Чек-лист развертывания

### Перед развертыванием
- [ ] SSL сертификаты настроены
- [ ] CSP заголовки обновлены для ElevenLabs
- [ ] API ключи ElevenLabs настроены
- [ ] Переменные окружения обновлены
- [ ] Тестирование на staging среде

### После развертывания
- [ ] Проверка CSP заголовков
- [ ] Тестирование ElevenLabs API
- [ ] Проверка аудио функций
- [ ] Мониторинг ошибок в консоли
- [ ] Проверка производительности

### Регулярные проверки
- [ ] Валидность SSL сертификатов
- [ ] Обновление CSP заголовков
- [ ] Мониторинг API лимитов
- [ ] Проверка безопасности
- [ ] Обновление документации

---

**Версия:** 1.0  
**Дата:** 2024  
**Автор:** Команда разработки HR Recruiter  
**Статус:** Требования определены 