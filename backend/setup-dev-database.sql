-- =====================================================
-- Development Database Setup Script
-- Database: tminet
-- User: tminet
-- Password: 0a0b0c0D.
-- =====================================================

-- Run this script as MySQL root user:
-- mysql -u root -p < setup-dev-database.sql

-- Create the development database
CREATE DATABASE IF NOT EXISTS tminet 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- Drop existing user if exists (to avoid conflicts)
DROP USER IF EXISTS 'tminet'@'localhost';

-- Create the development user
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';

-- Grant all privileges on the development database
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';

-- Grant specific privileges explicitly
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, INDEX
  ON tminet.* TO 'tminet'@'localhost';

-- Apply privilege changes
FLUSH PRIVILEGES;

-- Switch to the database
USE tminet;

-- Show created database and user
SELECT 'Database created successfully!' AS Status;
SHOW DATABASES LIKE 'tminet';

SELECT 'User created successfully!' AS Status;
SELECT User, Host FROM mysql.user WHERE User='tminet';

-- Test connection string will be:
-- jdbc:mysql://localhost:3306/tminet?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
