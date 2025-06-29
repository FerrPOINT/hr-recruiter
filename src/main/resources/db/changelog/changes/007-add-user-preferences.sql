-- Добавление поля preferences в таблицу users
ALTER TABLE users ADD COLUMN preferences TEXT;

-- Обновление существующих записей с дефолтными настройками
UPDATE users SET preferences = '{}' WHERE preferences IS NULL;

-- Добавление комментария к полю
COMMENT ON COLUMN users.preferences IS 'JSON строка с пользовательскими настройками'; 