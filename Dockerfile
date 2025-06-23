# Многоэтапная сборка для оптимизации размера образа
FROM eclipse-temurin:22-jdk-alpine AS builder

# Устанавливаем необходимые пакеты
RUN apk add --no-cache git

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы конфигурации Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Копируем спецификацию OpenAPI
COPY api api

# Делаем gradlew исполняемым
RUN chmod +x gradlew

# Скачиваем зависимости (кэшируем для ускорения сборки)
RUN ./gradlew dependencies --no-daemon

# Копируем исходный код
COPY src src

# Генерация исходников OpenAPI
RUN ./gradlew openApiGenerate --no-daemon

# Форматирование исходников и сгенерированных файлов (Spotless)
RUN ./gradlew spotlessApply --no-daemon

# Собираем приложение
RUN ./gradlew build --no-daemon -x test

# Второй этап - создание runtime образа
FROM eclipse-temurin:22-jre-alpine

# Устанавливаем необходимые пакеты для runtime
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Создаем пользователя для безопасности
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR файл из builder этапа
COPY --from=builder /app/build/libs/*.jar app.jar

# Создаем директории для логов и временных файлов
RUN mkdir -p /app/logs /app/temp && \
    chown -R appuser:appgroup /app

# Переключаемся на непривилегированного пользователя
USER appuser

# Настройка JVM для контейнеров
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Настройка health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 