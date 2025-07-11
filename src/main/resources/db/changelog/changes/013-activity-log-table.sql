-- Migration: Create activity_log table for widgets
-- Description: Creates table for storing user activity feed for dashboard widgets

CREATE TABLE activity_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    entity_id BIGINT,
    entity_type VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Создаем индексы для быстрого поиска
CREATE INDEX idx_activity_log_created_at ON activity_log(created_at DESC);
CREATE INDEX idx_activity_log_type ON activity_log(activity_type);
CREATE INDEX idx_activity_log_user_id ON activity_log(user_id);
CREATE INDEX idx_activity_log_entity ON activity_log(entity_type, entity_id);

-- Добавляем комментарии
COMMENT ON TABLE activity_log IS 'Лента активности пользователей для виджетов';
COMMENT ON COLUMN activity_log.user_id IS 'ID пользователя, совершившего действие';
COMMENT ON COLUMN activity_log.activity_type IS 'Тип активности: interview, position, candidate, hired, report, login';
COMMENT ON COLUMN activity_log.title IS 'Заголовок активности';
COMMENT ON COLUMN activity_log.description IS 'Описание активности';
COMMENT ON COLUMN activity_log.entity_id IS 'ID связанной сущности';
COMMENT ON COLUMN activity_log.entity_type IS 'Тип связанной сущности';
COMMENT ON COLUMN activity_log.metadata IS 'Дополнительные данные в формате JSON';
COMMENT ON COLUMN activity_log.created_at IS 'Время создания записи'; 