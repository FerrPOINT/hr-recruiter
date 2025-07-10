-- Добавление уникальных индексов для кандидатов
-- Миграция: 009-add-unique-indexes-candidates
-- Приводит схему БД в соответствие с JPA аннотациями

-- Сначала найдем дубликаты и обновим связи
-- Для email дубликатов
WITH email_duplicates AS (
    SELECT email, MIN(id) as keep_id, array_agg(id) as all_ids
    FROM candidates 
    WHERE email IS NOT NULL AND email != '' 
    GROUP BY email 
    HAVING COUNT(*) > 1
)
UPDATE interviews 
SET candidate_id = ed.keep_id 
FROM email_duplicates ed 
WHERE candidate_id = ANY(ed.all_ids) 
AND candidate_id != ed.keep_id;

-- Для телефонных дубликатов
WITH phone_duplicates AS (
    SELECT phone, MIN(id) as keep_id, array_agg(id) as all_ids
    FROM candidates 
    WHERE phone IS NOT NULL AND phone != '' 
    GROUP BY phone 
    HAVING COUNT(*) > 1
)
UPDATE interviews 
SET candidate_id = pd.keep_id 
FROM phone_duplicates pd 
WHERE candidate_id = ANY(pd.all_ids) 
AND candidate_id != pd.keep_id;

-- Теперь удаляем дубликаты (связи уже обновлены)
-- Удаляем дубликаты по email
DELETE FROM candidates 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM candidates 
    WHERE email IS NOT NULL AND email != '' 
    GROUP BY email
) 
AND email IS NOT NULL AND email != '';

-- Удаляем дубликаты по телефону
DELETE FROM candidates 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM candidates 
    WHERE phone IS NOT NULL AND phone != '' 
    GROUP BY phone
) 
AND phone IS NOT NULL AND phone != '';

-- Удаляем старые не-уникальные индексы
DROP INDEX IF EXISTS idx_candidates_email;
DROP INDEX IF EXISTS idx_candidates_phone;

-- Добавляем уникальные индексы (приводим в соответствие с JPA)
CREATE UNIQUE INDEX idx_candidates_email_unique ON candidates (email) WHERE email IS NOT NULL AND email != '';
CREATE UNIQUE INDEX idx_candidates_phone_unique ON candidates (phone) WHERE phone IS NOT NULL AND phone != '';

-- Добавляем комментарии к миграции
COMMENT ON INDEX idx_candidates_email_unique IS 'Уникальный индекс на email кандидата (соответствует JPA аннотации)';
COMMENT ON INDEX idx_candidates_phone_unique IS 'Уникальный индекс на телефон кандидата (соответствует JPA аннотации)'; 