-- Add error_message column to audit_logs table (if not already present)
-- MySQL 5.5 doesn't support IF NOT EXISTS, so we use a workaround
SET @dbname = DATABASE();
SET @tablename = "audit_logs";
SET @columnname = "error_message";
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE 
    (TABLE_NAME = @tablename) AND (TABLE_SCHEMA = @dbname) AND (COLUMN_NAME = @columnname)) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " LONGTEXT")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
