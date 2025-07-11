-- Добавление тестовых данных для виджетов
-- Безопасное обновление существующих кандидатов с разными источниками
UPDATE candidates SET source = 'direct' WHERE id = 1 AND source IS NULL;
UPDATE candidates SET source = 'referral' WHERE id = 2 AND source IS NULL;
UPDATE candidates SET source = 'jobBoard' WHERE id = 3 AND source IS NULL;
UPDATE candidates SET source = 'social' WHERE id = 4 AND source IS NULL;
UPDATE candidates SET source = 'direct' WHERE id = 5 AND source IS NULL;
UPDATE candidates SET source = 'referral' WHERE id = 6 AND source IS NULL;

-- Безопасное обновление статусов кандидатов для разнообразия статистики
UPDATE candidates SET status = 'REJECTED' WHERE id = 1 AND status = 'NEW';
UPDATE candidates SET status = 'HIRED' WHERE id = 2 AND status = 'NEW';
UPDATE candidates SET status = 'IN_PROGRESS' WHERE id = 3 AND status = 'NEW';
UPDATE candidates SET status = 'FINISHED' WHERE id = 4 AND status = 'NEW';

-- Безопасное обновление позиций с разными уровнями
UPDATE positions SET level = 'JUNIOR' WHERE id = 1 AND level IS NULL;
UPDATE positions SET level = 'MIDDLE' WHERE id = 2 AND level IS NULL;
UPDATE positions SET level = 'SENIOR' WHERE id = 3 AND level IS NULL;
UPDATE positions SET level = 'LEAD' WHERE id = 4 AND level IS NULL;

-- Безопасное добавление тестовых записей активности (только если их еще нет)
INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'position', 'Создана новая вакансия', 'Создана вакансия Java Developer', 1, 'position', '{"positionId": 1, "title": "Java Developer"}', NOW() - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'position' AND entity_id = 1 AND title = 'Создана новая вакансия');

INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'candidate', 'Добавлен новый кандидат', 'Добавлен кандидат Иван Иванов', 1, 'candidate', '{"candidateId": 1, "name": "Иван Иванов"}', NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'candidate' AND entity_id = 1 AND title = 'Добавлен новый кандидат');

INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'interview', 'Начато собеседование', 'Начато собеседование с кандидатом', 1, 'interview', '{"interviewId": 1, "candidateId": 1}', NOW() - INTERVAL '12 hours'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'interview' AND entity_id = 1 AND title = 'Начато собеседование');

INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'hired', 'Кандидат принят', 'Кандидат Петр Петров принят на работу', 2, 'candidate', '{"candidateId": 2, "name": "Петр Петров"}', NOW() - INTERVAL '6 hours'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'hired' AND entity_id = 2 AND title = 'Кандидат принят');

INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'position', 'Вакансия приостановлена', 'Вакансия Frontend Developer приостановлена', 2, 'position', '{"positionId": 2, "title": "Frontend Developer"}', NOW() - INTERVAL '3 hours'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'position' AND entity_id = 2 AND title = 'Вакансия приостановлена');

INSERT INTO activity_log (user_id, activity_type, title, description, entity_id, entity_type, metadata, created_at)
SELECT 1, 'candidate', 'Кандидат отклонен', 'Кандидат отклонен после собеседования', 1, 'candidate', '{"candidateId": 1, "name": "Иван Иванов"}', NOW() - INTERVAL '1 hour'
WHERE NOT EXISTS (SELECT 1 FROM activity_log WHERE activity_type = 'candidate' AND entity_id = 1 AND title = 'Кандидат отклонен'); 