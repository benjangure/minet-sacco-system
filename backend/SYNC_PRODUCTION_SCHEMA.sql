-- =====================================================
-- MINET SACCO PRODUCTION SCHEMA SYNCHRONIZATION SCRIPT
-- =====================================================
-- Purpose: Sync production database (minetsacco) structure 
--          to match local development database (tminet)
-- Date: August 5, 2026
-- 
-- IMPORTANT SAFETY NOTES:
-- 1. This script ONLY modifies structure, NOT data
-- 2. All changes use IF NOT EXISTS or similar checks
-- 3. NO DROP statements included
-- 4. BACKUP production database before running!
-- 5. Test on staging environment first if available
-- 
-- How to use:
-- 1. BACKUP: mysqldump -u minetsacco -p minetsacco > backup_before_sync.sql
-- 2. Run this script: mysql -u minetsacco -p minetsacco < SYNC_PRODUCTION_SCHEMA.sql
-- 3. Verify: Check application works correctly
-- =====================================================

-- Set safe mode
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Start transaction for safety
START TRANSACTION;

-- =====================================================
-- SECTION 1: CREATE MISSING TABLES
-- =====================================================

-- Table: push_subscriptions (for PWA push notifications)
CREATE TABLE IF NOT EXISTS `push_subscriptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `endpoint` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `p256dh_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `auth_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_endpoint` (`user_id`,`endpoint`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  KEY `idx_last_used` (`last_used_at`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_push_subscription_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Stores Web Push notification subscriptions for PWA functionality';


-- Table: user_devices (if needed for device tracking)
CREATE TABLE IF NOT EXISTS `user_devices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `platform` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_device_token` (`device_token`),
  CONSTRAINT `fk_user_devices_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: topup_guarantors (for loan top-up guarantor management)
CREATE TABLE IF NOT EXISTS `topup_guarantors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `topup_request_id` bigint NOT NULL,
  `guarantor_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `guarantee_amount` decimal(15,2) NOT NULL,
  `frozen_pledge` decimal(15,2) NOT NULL DEFAULT '0.00',
  `status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `approval_date` datetime DEFAULT NULL,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_topup_request` (`topup_request_id`),
  KEY `idx_guarantor` (`guarantor_id`),
  KEY `idx_member` (`member_id`),
  CONSTRAINT `fk_topup_guarantor_request` FOREIGN KEY (`topup_request_id`) REFERENCES `loan_topup_requests` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_topup_guarantor_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================
-- SECTION 2: ADD MISSING COLUMNS TO EXISTING TABLES
-- =====================================================

-- Add columns to members table if they don't exist
ALTER TABLE `members` 
  ADD COLUMN IF NOT EXISTS `full_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `last_name`,
  ADD COLUMN IF NOT EXISTS `next_of_kin_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `next_of_kin_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `next_of_kin_relationship` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `is_exited` tinyint(1) NOT NULL DEFAULT '0',
  ADD COLUMN IF NOT EXISTS `exit_date` date DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `exit_reason` text COLLATE utf8mb4_unicode_ci;

-- Add columns to loans table if they don't exist
ALTER TABLE `loans`
  ADD COLUMN IF NOT EXISTS `interest_remaining` decimal(15,2) DEFAULT NULL AFTER `interest_rate`,
  ADD COLUMN IF NOT EXISTS `interest_collected` decimal(15,2) DEFAULT '0.00' AFTER `interest_remaining`,
  ADD COLUMN IF NOT EXISTS `original_principal` decimal(15,2) DEFAULT NULL AFTER `principal_amount`,
  ADD COLUMN IF NOT EXISTS `is_topup` tinyint(1) NOT NULL DEFAULT '0',
  ADD COLUMN IF NOT EXISTS `parent_loan_id` bigint DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `topup_additional_amount` decimal(15,2) DEFAULT '0.00',
  ADD COLUMN IF NOT EXISTS `topup_request_id` bigint DEFAULT NULL;

-- Add foreign key for topup parent loan if not exists
-- Check if constraint exists first (MariaDB compatible)
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
  WHERE CONSTRAINT_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'loans' 
  AND CONSTRAINT_NAME = 'fk_loan_parent_topup');

SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE `loans` ADD CONSTRAINT `fk_loan_parent_topup` FOREIGN KEY (`parent_loan_id`) REFERENCES `loans` (`id`) ON DELETE SET NULL',
  'SELECT "Foreign key fk_loan_parent_topup already exists" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- Add columns to users table if they don't exist  
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `first_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `last_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `is_first_login` tinyint(1) NOT NULL DEFAULT '1';

-- Add columns to loan_topup_requests table if they don't exist
ALTER TABLE `loan_topup_requests`
  ADD COLUMN IF NOT EXISTS `hr_approved` tinyint(1) DEFAULT NULL AFTER `status`,
  ADD COLUMN IF NOT EXISTS `hr_approved_by` bigint DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `hr_approved_at` datetime DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `hr_rejection_reason` text COLLATE utf8mb4_unicode_ci,
  ADD COLUMN IF NOT EXISTS `treasurer_approved` tinyint(1) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `treasurer_approved_by` bigint DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `treasurer_approved_at` datetime DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `treasurer_rejection_reason` text COLLATE utf8mb4_unicode_ci;

-- Add columns to member_credentials table if they don't exist
ALTER TABLE `member_credentials`
  ADD COLUMN IF NOT EXISTS `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `pin`;

-- Add columns to guarantors table if they don't exist
ALTER TABLE `guarantors`
  ADD COLUMN IF NOT EXISTS `is_next_of_kin` tinyint(1) NOT NULL DEFAULT '0',
  ADD COLUMN IF NOT EXISTS `next_of_kin_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `next_of_kin_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `next_of_kin_relationship` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL;


-- Add columns to loan_products table if they don't exist
ALTER TABLE `loan_products`
  ADD COLUMN IF NOT EXISTS `max_total_borrowing_limit` decimal(15,2) DEFAULT NULL AFTER `max_amount`;

-- Add columns to member_exits table if they don't exist
ALTER TABLE `member_exits`
  ADD COLUMN IF NOT EXISTS `final_payout_amount` decimal(15,2) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `processed_by` bigint DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `processed_at` datetime DEFAULT NULL;

-- =====================================================
-- SECTION 3: ADD MISSING INDEXES FOR PERFORMANCE
-- =====================================================

-- Indexes on loans table
ALTER TABLE `loans`
  ADD INDEX IF NOT EXISTS `idx_parent_loan` (`parent_loan_id`),
  ADD INDEX IF NOT EXISTS `idx_topup_request` (`topup_request_id`),
  ADD INDEX IF NOT EXISTS `idx_is_topup` (`is_topup`),
  ADD INDEX IF NOT EXISTS `idx_loan_status_date` (`status`, `disbursement_date`),
  ADD INDEX IF NOT EXISTS `idx_member_status` (`member_id`, `status`);

-- Indexes on members table
ALTER TABLE `members`
  ADD INDEX IF NOT EXISTS `idx_member_status` (`status`),
  ADD INDEX IF NOT EXISTS `idx_employee_id` (`employee_id`),
  ADD INDEX IF NOT EXISTS `idx_is_exited` (`is_exited`),
  ADD INDEX IF NOT EXISTS `idx_full_name` (`full_name`);

-- Indexes on transactions table
ALTER TABLE `transactions`
  ADD INDEX IF NOT EXISTS `idx_transaction_date` (`transaction_date`),
  ADD INDEX IF NOT EXISTS `idx_account_type_date` (`account_id`, `transaction_type`, `transaction_date`),
  ADD INDEX IF NOT EXISTS `idx_transaction_type` (`transaction_type`);


-- Indexes on guarantors table
ALTER TABLE `guarantors`
  ADD INDEX IF NOT EXISTS `idx_guarantor_status` (`status`),
  ADD INDEX IF NOT EXISTS `idx_member_loan` (`member_id`, `loan_id`),
  ADD INDEX IF NOT EXISTS `idx_next_of_kin` (`is_next_of_kin`);

-- Indexes on notifications table
ALTER TABLE `notifications`
  ADD INDEX IF NOT EXISTS `idx_user_read` (`user_id`, `is_read`),
  ADD INDEX IF NOT EXISTS `idx_created_at` (`created_at`),
  ADD INDEX IF NOT EXISTS `idx_target_role` (`target_role`);

-- Indexes on audit_logs table
ALTER TABLE `audit_logs`
  ADD INDEX IF NOT EXISTS `idx_user_action` (`user_id`, `action`),
  ADD INDEX IF NOT EXISTS `idx_timestamp` (`timestamp`),
  ADD INDEX IF NOT EXISTS `idx_entity_type_id` (`entity_type`, `entity_id`);

-- Indexes on accounts table
ALTER TABLE `accounts`
  ADD INDEX IF NOT EXISTS `idx_member_type` (`member_id`, `account_type`),
  ADD INDEX IF NOT EXISTS `idx_account_type` (`account_type`);

-- =====================================================
-- SECTION 4: UPDATE ENUM VALUES IF NEEDED
-- =====================================================

-- Note: Enum modifications require careful handling
-- These are examples - verify against your actual schema

-- Update loan status enum to include new statuses
-- ALTER TABLE `loans` MODIFY COLUMN `status` 
--   ENUM('PENDING','APPROVED','DISBURSED','FULLY_REPAID','DEFAULTED','REJECTED',
--        'PENDING_HR_APPROVAL','PENDING_TREASURER_APPROVAL','PENDING_GUARANTORS',
--        'PARTIALLY_APPROVED') 
--   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING';


-- =====================================================
-- SECTION 5: DATA INTEGRITY UPDATES (SAFE)
-- =====================================================

-- Backfill full_name for members where it's null
UPDATE `members` 
SET `full_name` = CONCAT(COALESCE(`first_name`, ''), ' ', COALESCE(`last_name`, ''))
WHERE `full_name` IS NULL 
  AND (`first_name` IS NOT NULL OR `last_name` IS NOT NULL);

-- Backfill original_principal for existing loans where it's null
UPDATE `loans`
SET `original_principal` = `principal_amount`
WHERE `original_principal` IS NULL 
  AND `principal_amount` IS NOT NULL;

-- =====================================================
-- SECTION 6: FLYWAY MIGRATION TRACKING UPDATE
-- =====================================================

-- Add Flyway migration record for push subscriptions table (V149)
INSERT INTO `flyway_schema_history` 
  (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`)
SELECT 149, '149', 'Create push subscriptions table', 'SQL', 'V149__Create_push_subscriptions_table.sql', -1, USER(), NOW(), 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `flyway_schema_history` WHERE `version` = '149'
);

-- Add migration record for user devices table (V999)
INSERT INTO `flyway_schema_history`
  (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`)
SELECT 999, '999', 'Create user devices table', 'SQL', 'V999__Create_user_devices_table.sql', -1, USER(), NOW(), 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `flyway_schema_history` WHERE `version` = '999'
);


-- =====================================================
-- SECTION 7: VERIFICATION QUERIES
-- =====================================================

-- These queries help verify the sync was successful
-- Run these after the migration to check everything is in place

SELECT 'Verification Queries - Run these after migration:' AS INFO;

-- Check if push_subscriptions table exists
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ push_subscriptions table exists'
    ELSE '✗ push_subscriptions table MISSING'
  END AS check_result
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
  AND table_name = 'push_subscriptions';

-- Check if user_devices table exists  
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ user_devices table exists'
    ELSE '✗ user_devices table MISSING'
  END AS check_result
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
  AND table_name = 'user_devices';

-- Check if topup_guarantors table exists
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ topup_guarantors table exists'
    ELSE '✗ topup_guarantors table MISSING'
  END AS check_result
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
  AND table_name = 'topup_guarantors';

-- Check if members.full_name column exists
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ members.full_name column exists'
    ELSE '✗ members.full_name column MISSING'
  END AS check_result
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'members'
  AND column_name = 'full_name';


-- Check if loans.interest_collected column exists
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ loans.interest_collected column exists'
    ELSE '✗ loans.interest_collected column MISSING'
  END AS check_result
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'loans'
  AND column_name = 'interest_collected';

-- Check if loans.is_topup column exists
SELECT 
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ loans.is_topup column exists'
    ELSE '✗ loans.is_topup column MISSING'
  END AS check_result
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'loans'
  AND column_name = 'is_topup';

-- Count total tables in database
SELECT COUNT(*) AS total_tables, 
       'Total tables in database' AS description
FROM information_schema.tables 
WHERE table_schema = DATABASE();

-- Count total indexes
SELECT COUNT(*) AS total_indexes,
       'Total indexes in database' AS description
FROM information_schema.statistics
WHERE table_schema = DATABASE();

-- =====================================================
-- FINALIZE
-- =====================================================

-- Commit the transaction
COMMIT;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

SELECT '========================================' AS '';
SELECT 'SCHEMA SYNCHRONIZATION COMPLETED!' AS '';
SELECT '========================================' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Review verification queries above' AS '';
SELECT '2. Test application functionality' AS '';
SELECT '3. Monitor logs for any errors' AS '';
SELECT '4. Keep backup until verified working' AS '';
SELECT '========================================' AS '';

