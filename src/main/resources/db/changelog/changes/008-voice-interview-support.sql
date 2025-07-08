-- Миграция для поддержки голосового интервью с ElevenLabs Conversational AI
-- Добавляет поля для голосовых сессий и ответов

-- Добавляем поля для голосового интервью в таблицу interviews
ALTER TABLE interviews ADD COLUMN voice_session_id VARCHAR(255);
ALTER TABLE interviews ADD COLUMN voice_agent_id VARCHAR(255);
ALTER TABLE interviews ADD COLUMN voice_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE interviews ADD COLUMN voice_language VARCHAR(10);
ALTER TABLE interviews ADD COLUMN voice_voice_id VARCHAR(255);
ALTER TABLE interviews ADD COLUMN voice_started_at TIMESTAMP;
ALTER TABLE interviews ADD COLUMN voice_finished_at TIMESTAMP;
ALTER TABLE interviews ADD COLUMN voice_total_duration BIGINT;

-- Добавляем поля для голосовых ответов в таблицу interview_answers
ALTER TABLE interview_answers ADD COLUMN voice_session_id VARCHAR(255);
ALTER TABLE interview_answers ADD COLUMN voice_confidence DOUBLE PRECISION;
ALTER TABLE interview_answers ADD COLUMN voice_emotion VARCHAR(50);
ALTER TABLE interview_answers ADD COLUMN voice_speaker_id VARCHAR(255);
ALTER TABLE interview_answers ADD COLUMN voice_audio_url VARCHAR(500);
ALTER TABLE interview_answers ADD COLUMN voice_processing_time BIGINT;
ALTER TABLE interview_answers ADD COLUMN voice_quality_score DOUBLE PRECISION;

-- Создаем индексы для оптимизации запросов
CREATE INDEX idx_interviews_voice_session_id ON interviews(voice_session_id);
CREATE INDEX idx_interviews_voice_enabled ON interviews(voice_enabled);
CREATE INDEX idx_interviews_voice_agent_id ON interviews(voice_agent_id);
CREATE INDEX idx_interviews_voice_language ON interviews(voice_language);
CREATE INDEX idx_interviews_voice_started_at ON interviews(voice_started_at);
CREATE INDEX idx_interviews_voice_finished_at ON interviews(voice_finished_at);

CREATE INDEX idx_interview_answers_voice_session_id ON interview_answers(voice_session_id);
CREATE INDEX idx_interview_answers_voice_confidence ON interview_answers(voice_confidence);
CREATE INDEX idx_interview_answers_voice_emotion ON interview_answers(voice_emotion);
CREATE INDEX idx_interview_answers_voice_speaker_id ON interview_answers(voice_speaker_id);
CREATE INDEX idx_interview_answers_voice_processing_time ON interview_answers(voice_processing_time);
CREATE INDEX idx_interview_answers_voice_quality_score ON interview_answers(voice_quality_score);

-- Добавляем комментарии к полям
COMMENT ON COLUMN interviews.voice_session_id IS 'ID сессии в ElevenLabs Conversational AI';
COMMENT ON COLUMN interviews.voice_agent_id IS 'ID агента в ElevenLabs';
COMMENT ON COLUMN interviews.voice_enabled IS 'Включено ли голосовое интервью';
COMMENT ON COLUMN interviews.voice_language IS 'Язык голосового интервью (ISO 639-1)';
COMMENT ON COLUMN interviews.voice_voice_id IS 'ID голоса в ElevenLabs';
COMMENT ON COLUMN interviews.voice_started_at IS 'Время начала голосовой сессии';
COMMENT ON COLUMN interviews.voice_finished_at IS 'Время завершения голосовой сессии';
COMMENT ON COLUMN interviews.voice_total_duration IS 'Общая длительность голосового интервью в секундах';

COMMENT ON COLUMN interview_answers.voice_session_id IS 'ID голосовой сессии для этого ответа';
COMMENT ON COLUMN interview_answers.voice_confidence IS 'Уверенность в распознавании речи (0.0-1.0)';
COMMENT ON COLUMN interview_answers.voice_emotion IS 'Определенная эмоция в голосе';
COMMENT ON COLUMN interview_answers.voice_speaker_id IS 'ID говорящего (если несколько)';
COMMENT ON COLUMN interview_answers.voice_audio_url IS 'URL аудио файла ответа';
COMMENT ON COLUMN interview_answers.voice_processing_time IS 'Время обработки в миллисекундах';
COMMENT ON COLUMN interview_answers.voice_quality_score IS 'Оценка качества аудио (0.0-1.0)'; 