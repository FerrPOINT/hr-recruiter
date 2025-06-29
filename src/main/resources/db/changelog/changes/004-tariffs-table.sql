-- Создание таблицы для тарифных планов
CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    max_positions INTEGER,
    max_candidates INTEGER,
    max_interviews INTEGER,
    ai_features_enabled BOOLEAN DEFAULT false,
    priority_support BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы для функций тарифов
CREATE TABLE tariff_features (
    tariff_id BIGINT NOT NULL,
    feature VARCHAR(200) NOT NULL,
    PRIMARY KEY (tariff_id, feature),
    FOREIGN KEY (tariff_id) REFERENCES tariffs(id) ON DELETE CASCADE
);

-- Создание индексов
CREATE INDEX idx_tariffs_name ON tariffs(name);
CREATE INDEX idx_tariffs_is_active ON tariffs(is_active);
CREATE INDEX idx_tariffs_price ON tariffs(price);
CREATE INDEX idx_tariffs_ai_features ON tariffs(ai_features_enabled);

-- Добавление дефолтных тарифов
INSERT INTO tariffs (
    name,
    description,
    price,
    is_active,
    max_positions,
    max_candidates,
    max_interviews,
    ai_features_enabled,
    priority_support,
    created_at,
    updated_at
) VALUES 
    (
        'Базовый',
        'Базовый тариф для начала работы с системой',
        0.00,
        true,
        5,
        50,
        100,
        false,
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Стандарт',
        'Стандартный тариф для растущих компаний',
        29.99,
        true,
        20,
        200,
        500,
        true,
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Профессиональный',
        'Профессиональный тариф для крупных компаний',
        99.99,
        true,
        100,
        1000,
        2500,
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Корпоративный',
        'Корпоративный тариф для больших организаций',
        299.99,
        true,
        NULL,
        NULL,
        NULL,
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

-- Добавление функций для тарифов
INSERT INTO tariff_features (tariff_id, feature) VALUES
    (1, 'Создание вакансий'),
    (1, 'Управление кандидатами'),
    (1, 'Проведение собеседований'),
    (1, 'Базовые отчеты'),
    
    (2, 'Все функции Базового'),
    (2, 'AI-генерация вопросов'),
    (2, 'Транскрибация аудио'),
    (2, 'Расширенная аналитика'),
    (2, 'Экспорт данных'),
    
    (3, 'Все функции Стандартного'),
    (3, 'Приоритетная поддержка'),
    (3, 'Интеграции с HR-системами'),
    (3, 'Кастомные отчеты'),
    (3, 'API доступ'),
    
    (4, 'Все функции Профессионального'),
    (4, 'Неограниченные лимиты'),
    (4, 'Персональный менеджер'),
    (4, 'Кастомизация системы'),
    (4, 'Обучение команды');

-- Если таблица tariffs уже существует и поле price имеет другой тип, выполнить:
-- ALTER TABLE tariffs ALTER COLUMN price TYPE DECIMAL(10,2) USING price::DECIMAL(10,2); 