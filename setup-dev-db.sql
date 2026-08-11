-- Setup Development Database
-- This creates the tminet database and user

DROP DATABASE IF EXISTS tminet;
CREATE DATABASE tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;

SELECT 'Database and user created successfully!' AS Status;
