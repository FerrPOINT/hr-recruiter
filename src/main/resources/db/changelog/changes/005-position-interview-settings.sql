-- Добавление полей настроек проведения собеседования в таблицу positions
ALTER TABLE positions 
ADD COLUMN language VARCHAR(10),
ADD COLUMN show_other_lang BOOLEAN DEFAULT FALSE,
ADD COLUMN answer_time INTEGER,
ADD COLUMN level VARCHAR(20),
ADD COLUMN save_audio BOOLEAN DEFAULT TRUE,
ADD COLUMN save_video BOOLEAN DEFAULT FALSE,
ADD COLUMN random_order BOOLEAN DEFAULT FALSE,
ADD COLUMN question_type VARCHAR(50),
ADD COLUMN questions_count INTEGER,
ADD COLUMN check_type VARCHAR(50);

-- Добавление комментариев к новым полям
COMMENT ON COLUMN positions.language IS 'Язык собеседования';
COMMENT ON COLUMN positions.show_other_lang IS 'Показывать вопросы на других языках';
COMMENT ON COLUMN positions.answer_time IS 'Время ответа на вопрос в секундах';
COMMENT ON COLUMN positions.level IS 'Уровень позиции (JUNIOR, MIDDLE, SENIOR, LEAD)';
COMMENT ON COLUMN positions.save_audio IS 'Сохранять аудио ответы';
COMMENT ON COLUMN positions.save_video IS 'Сохранять видео ответы';
COMMENT ON COLUMN positions.random_order IS 'Случайный порядок вопросов';
COMMENT ON COLUMN positions.question_type IS 'Тип вопросов';
COMMENT ON COLUMN positions.questions_count IS 'Количество вопросов';
COMMENT ON COLUMN positions.check_type IS 'Тип проверки ответов'; 