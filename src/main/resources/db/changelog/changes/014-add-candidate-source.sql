-- Migration: Add source field to candidates table
-- Description: Adds source field for candidate grouping in widgets

-- Добавляем поле source в таблицу candidates
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS source VARCHAR(50);

-- Создаем индекс для быстрого поиска по источнику
CREATE INDEX IF NOT EXISTS idx_candidates_source ON candidates(source);

-- Добавляем комментарий
COMMENT ON COLUMN candidates.source IS 'Источник кандидата: direct, referral, jobBoard, social';

-- Обновляем существующие записи (если есть)
UPDATE candidates SET source = 'direct' WHERE source IS NULL; 