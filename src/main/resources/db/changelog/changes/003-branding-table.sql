-- Создание таблицы для настроек брендинга
CREATE TABLE branding (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    logo_url VARCHAR(500),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    email_signature TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов
CREATE INDEX idx_branding_company_name ON branding(company_name);
CREATE INDEX idx_branding_created_at ON branding(created_at);

-- Добавление дефолтных настроек брендинга
INSERT INTO branding (
    company_name,
    logo_url,
    primary_color,
    secondary_color,
    email_signature,
    created_at,
    updated_at
) VALUES (
    'HR Recruiter',
    NULL,
    '#2563eb',
    '#64748b',
    'С уважением,\nКоманда HR Recruiter',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
); 