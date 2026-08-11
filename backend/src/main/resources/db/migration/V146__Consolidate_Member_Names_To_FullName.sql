-- =====================================================
-- Migration V146: Consolidate Member Names to Full Name
-- =====================================================
-- This migration consolidates firstName, middleName, lastName into a single fullName column
-- and migrates existing data

-- Step 1: Add the new fullName column (only if it doesn't exist)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'full_name');

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE members ADD COLUMN full_name VARCHAR(150) AFTER first_name', 
    'SELECT "Column full_name already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Migrate existing data - concatenate firstName and lastName (trim spaces)
-- Using id > 0 to satisfy MySQL safe update mode
UPDATE members 
SET full_name = TRIM(CONCAT(
    COALESCE(first_name, ''),
    ' ',
    COALESCE(last_name, '')
))
WHERE (full_name IS NULL OR full_name = '') AND id > 0;

-- Step 3: Handle cases where firstName already contains the full name
-- Using id > 0 to satisfy MySQL safe update mode
UPDATE members 
SET full_name = TRIM(first_name)
WHERE (full_name IS NULL OR full_name = '' OR full_name = ' ') AND id > 0;

-- Step 4: Make fullName NOT NULL after migration (only if not already NOT NULL)
SET @is_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'full_name');

SET @sql = IF(@is_nullable = 'YES', 
    'ALTER TABLE members MODIFY COLUMN full_name VARCHAR(150) NOT NULL', 
    'SELECT "Column full_name is already NOT NULL" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 5: Add index on fullName for searching (only if it doesn't exist)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND INDEX_NAME = 'idx_members_full_name');

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_members_full_name ON members(full_name)', 
    'SELECT "Index idx_members_full_name already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 6: Drop the old lastName column (we'll keep firstName temporarily for backward compatibility)
-- We'll drop firstName in a future migration after ensuring all code is updated
-- Only drop if it exists
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'last_name');

SET @sql = IF(@col_exists > 0, 
    'ALTER TABLE members DROP COLUMN last_name', 
    'SELECT "Column last_name already dropped" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 7: Create a view for backward compatibility (optional, can be removed later)
-- This allows old queries to still work temporarily
CREATE OR REPLACE VIEW members_legacy_view AS
SELECT 
    id,
    member_number,
    employee_id,
    full_name AS first_name,
    '' AS last_name,
    full_name,
    email,
    phone,
    national_id,
    date_of_birth,
    employment_status,
    employer,
    department,
    status,
    kyc_completion_status,
    kyc_completed_at,
    kyc_verified_at,
    id_document_path,
    photo_path,
    application_letter_path,
    kra_pin_path,
    bank_name,
    bank_account_number,
    bank_branch,
    next_of_kin_name,
    next_of_kin_phone,
    next_of_kin_relationship,
    created_by,
    approved_by,
    approved_at,
    rejection_reason,
    exit_date,
    exit_reason,
    consecutive_months_counter,
    migration_status,
    is_legacy_member,
    created_at,
    updated_at
FROM members;

-- Verification queries (commented out - run these manually to verify)
-- SELECT member_number, first_name, full_name FROM members LIMIT 20;
-- SELECT COUNT(*) FROM members WHERE full_name IS NULL OR full_name = '';
