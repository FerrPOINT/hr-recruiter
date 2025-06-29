-- Добавление поля для обоснования оценки в таблицу interview_answers
ALTER TABLE interview_answers ADD COLUMN score_justification TEXT;

-- Удаление избыточного поля transcript (оставляем raw_transcription и formatted_transcription)
ALTER TABLE interview_answers DROP COLUMN IF EXISTS transcript;

-- Удаление избыточных полей из таблицы interviews (информация хранится в interview_answers)
ALTER TABLE interviews DROP COLUMN IF EXISTS transcript;
ALTER TABLE interviews DROP COLUMN IF EXISTS audio_url;
ALTER TABLE interviews DROP COLUMN IF EXISTS video_url; 