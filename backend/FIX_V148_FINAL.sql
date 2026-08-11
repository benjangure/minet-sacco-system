-- ============================================================
-- FINAL FIX FOR V148 MIGRATION ISSUE
-- Run this script to permanently resolve the migration problem
-- ============================================================

SET SQL_SAFE_UPDATES = 0;

-- Step 1: Delete any failed V148 entry
DELETE FROM flyway_schema_history WHERE version = '148';

-- Step 2: Check if columns already exist and add only if missing
SET @dbname = DATABASE();
SET @tablename = 'members';
SET @columnname = 'exited_by';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1', -- Column exists, do nothing
  'ALTER TABLE members ADD COLUMN exited_by BIGINT NULL COMMENT ''User ID who marked member as exited'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

SET @columnname = 'exit_notes';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1', -- Column exists, do nothing
  'ALTER TABLE members ADD COLUMN exit_notes TEXT NULL COMMENT ''Additional notes about the exit'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

-- Step 3: Check and add guarantors columns
SET @tablename = 'guarantors';
SET @columnname = 'replaced_at';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE guarantors ADD COLUMN replaced_at DATETIME NULL COMMENT ''When this guarantor was replaced'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

SET @columnname = 'replaced_by_guarantor_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE guarantors ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT ''ID of guarantor who replaced this one'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

SET @columnname = 'replacement_reason';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE guarantors ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT ''Reason for replacement'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

-- Step 4: Check and add topup_guarantors columns
SET @tablename = 'topup_guarantors';
SET @columnname = 'replaced_at';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE topup_guarantors ADD COLUMN replaced_at DATETIME NULL COMMENT ''When this guarantor was replaced'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

SET @columnname = 'replaced_by_guarantor_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE topup_guarantors ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT ''ID of guarantor who replaced this one'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

SET @columnname = 'replacement_reason';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE topup_guarantors ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT ''Reason for replacement'''
));
PREPARE addColumnIfNotExists FROM @preparedStatement;
EXECUTE addColumnIfNotExists;
DEALLOCATE PREPARE addColumnIfNotExists;

-- Step 5: Add indexes if they don't exist
SET @indexname = 'idx_guarantors_replaced_at';
SET @tablename = 'guarantors';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND INDEX_NAME = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_guarantors_replaced_at ON guarantors(replaced_at)'
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

SET @indexname = 'idx_topup_guarantors_replaced_at';
SET @tablename = 'topup_guarantors';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND INDEX_NAME = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_topup_guarantors_replaced_at ON topup_guarantors(replaced_at)'
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- Step 6: Mark V148 as successfully applied
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    COALESCE(MAX(installed_rank), 0) + 1,
    '148',
    'Add member exit tracking',
    'SQL',
    'V148__Add_member_exit_tracking.sql',
    NULL,
    USER(),
    NOW(),
    1,
    1
FROM flyway_schema_history;

SET SQL_SAFE_UPDATES = 1;

SELECT 'V148 migration fixed successfully!' AS status;
