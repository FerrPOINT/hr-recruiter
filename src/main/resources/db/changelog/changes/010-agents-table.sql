-- Migration: Create agents table for AI agents management
-- Description: Creates table for storing AI agents configuration and status

CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    elevenlabs_agent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATING',
    config JSONB,
    interview_id BIGINT,
    position_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создаем индексы
CREATE INDEX idx_agents_status ON agents (status);
CREATE INDEX idx_agents_interview_id ON agents (interview_id);
CREATE INDEX idx_agents_position_id ON agents (position_id);
CREATE INDEX idx_agents_elevenlabs_id ON agents (elevenlabs_agent_id);
CREATE INDEX idx_agents_created_at ON agents (created_at);

-- Создаем внешние ключи
ALTER TABLE agents 
    ADD CONSTRAINT fk_agents_interview_id 
    FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE SET NULL;

ALTER TABLE agents 
    ADD CONSTRAINT fk_agents_position_id 
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE SET NULL;

-- Добавляем комментарии к таблице и колонкам
COMMENT ON TABLE agents IS 'AI агенты для голосовых интервью';
COMMENT ON COLUMN agents.name IS 'Название агента';
COMMENT ON COLUMN agents.description IS 'Описание агента';
COMMENT ON COLUMN agents.elevenlabs_agent_id IS 'ID агента в ElevenLabs';
COMMENT ON COLUMN agents.status IS 'Статус агента: ACTIVE, INACTIVE, DELETED, ERROR, CREATING';
COMMENT ON COLUMN agents.config IS 'Конфигурация агента в формате JSON';
COMMENT ON COLUMN agents.interview_id IS 'ID интервью, для которого создан агент';
COMMENT ON COLUMN agents.position_id IS 'ID позиции';
COMMENT ON COLUMN agents.created_at IS 'Время создания';
COMMENT ON COLUMN agents.updated_at IS 'Время обновления'; 