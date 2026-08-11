-- Fix V148 Migration Failure
-- Run this script manually in MySQL Workbench or command line

-- Step 1: Remove the failed V148 entry from flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = '148';

-- Step 2: Now restart the backend and it will run V148 correctly
-- Or you can run the fixed migration manually:

-- Add new columns for member exit tracking
ALTER TABLE members
ADD COLUMN exited_by BIGINT NULL COMMENT 'User ID who marked member as exited',
ADD COLUMN exit_notes TEXT NULL COMMENT 'Additional notes about the exit';

-- Add indexes
CREATE INDEX idx_members_exit_date ON members(exit_date);
CREATE INDEX idx_members_exit_status ON members(status, exit_date);

-- Add guarantor replacement tracking columns
ALTER TABLE guarantors
ADD COLUMN replaced_at DATETIME NULL COMMENT 'When this guarantor was replaced',
ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT 'ID of guarantor who replaced this one',
ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT 'Reason for replacement: MEMBER_EXIT, REJECTION, etc';

CREATE INDEX idx_guarantors_replaced_at ON guarantors(replaced_at);

ALTER TABLE topup_guarantors
ADD COLUMN replaced_at DATETIME NULL COMMENT 'When this guarantor was replaced',
ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT 'ID of guarantor who replaced this one',
ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT 'Reason for replacement: MEMBER_EXIT, REJECTION, etc';

CREATE INDEX idx_topup_guarantors_replaced_at ON topup_guarantors(replaced_at);

-- Step 3: Mark V148 as successful in Flyway history
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '148',
    'Add member exit tracking',
    'SQL',
    'V148__Add_member_exit_tracking.sql',
    NULL,
    USER(),
    NOW(),
    1,
    1
);
