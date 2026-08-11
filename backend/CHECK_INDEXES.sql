-- =====================================================
-- Check if Performance Indexes Exist
-- =====================================================
-- Run this in MySQL Workbench to verify indexes were created
-- =====================================================

USE tminet;

-- Check all indexes that start with 'idx_'
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS INDEXED_COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'tminet'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- Count indexes per table
SELECT 
    TABLE_NAME,
    COUNT(DISTINCT INDEX_NAME) as INDEX_COUNT
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'tminet'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;

-- Check if specific critical indexes exist
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='tminet' AND TABLE_NAME='loans' AND INDEX_NAME='idx_loans_member_id') THEN 'EXISTS'
        ELSE 'MISSING'
    END as idx_loans_member_id,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='tminet' AND TABLE_NAME='loans' AND INDEX_NAME='idx_loans_status') THEN 'EXISTS'
        ELSE 'MISSING'
    END as idx_loans_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='tminet' AND TABLE_NAME='transactions' AND INDEX_NAME='idx_transactions_account_id') THEN 'EXISTS'
        ELSE 'MISSING'
    END as idx_transactions_account_id,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='tminet' AND TABLE_NAME='guarantors' AND INDEX_NAME='idx_guarantors_loan_id') THEN 'EXISTS'
        ELSE 'MISSING'
    END as idx_guarantors_loan_id;
