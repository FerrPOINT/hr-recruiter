-- Migration: Create agents table for AI agents management
-- Description: Creates table for storing AI agents configuration and status

CREATE TABLE agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT 'Название агента',
    description TEXT COMMENT 'Описание агента',
    elevenlabs_agent_id VARCHAR(255) COMMENT 'ID агента в ElevenLabs',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATING' COMMENT 'Статус агента: ACTIVE, INACTIVE, DELETED, ERROR, CREATING',
    config JSON COMMENT 'Конфигурация агента в формате JSON',
    interview_id BIGINT COMMENT 'ID интервью, для которого создан агент',
    position_id BIGINT COMMENT 'ID позиции',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время создания',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Время обновления',
    
    INDEX idx_agents_status (status),
    INDEX idx_agents_interview_id (interview_id),
    INDEX idx_agents_position_id (position_id),
    INDEX idx_agents_elevenlabs_id (elevenlabs_agent_id),
    INDEX idx_agents_created_at (created_at),
    
    FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE SET NULL,
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI агенты для голосовых интервью';

-- Добавляем комментарии к колонкам
ALTER TABLE agents 
    MODIFY COLUMN name VARCHAR(200) NOT NULL COMMENT 'Название агента',
    MODIFY COLUMN description TEXT COMMENT 'Описание агента',
    MODIFY COLUMN elevenlabs_agent_id VARCHAR(255) COMMENT 'ID агента в ElevenLabs',
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'CREATING' COMMENT 'Статус агента: ACTIVE, INACTIVE, DELETED, ERROR, CREATING',
    MODIFY COLUMN config JSON COMMENT 'Конфигурация агента в формате JSON',
    MODIFY COLUMN interview_id BIGINT COMMENT 'ID интервью, для которого создан агент',
    MODIFY COLUMN position_id BIGINT COMMENT 'ID позиции',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время создания',
    MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Время обновления'; 