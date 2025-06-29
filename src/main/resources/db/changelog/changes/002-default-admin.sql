-- Добавление дефолтного администратора
-- Пароль: admin (захешированный с помощью BCrypt)
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    role,
    is_active,
    language,
    created_at,
    updated_at
) VALUES (
    'Admin',
    'User',
    'admin@example.com',
    '$2a$10$fyjHPdBd.Y.MtW.fW.xrPuJAPrblubrZ076bST0XLEMnxJRw8AWum', -- admin
    'ADMIN',
    true,
    'ru',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
); 