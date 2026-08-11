-- =====================================================
-- SHOW DATABASE SCHEMA - Run this to see all tables and columns
-- =====================================================

-- Show all tables in the database
SELECT 'ALL TABLES IN DATABASE:' AS info;
SHOW TABLES;

-- Show structure of loan_topup_requests
SELECT 'LOAN_TOPUP_REQUESTS TABLE:' AS info;
DESCRIBE loan_topup_requests;

-- Show structure of topup_guarantors
SELECT 'TOPUP_GUARANTORS TABLE:' AS info;
DESCRIBE topup_guarantors;

-- Show structure of loans
SELECT 'LOANS TABLE:' AS info;
DESCRIBE loans;

-- Show structure of loan_guarantors
SELECT 'LOAN_GUARANTORS TABLE:' AS info;
DESCRIBE loan_guarantors;

-- Show structure of transactions
SELECT 'TRANSACTIONS TABLE:' AS info;
DESCRIBE transactions;

-- Show structure of mpesa_transactions
SELECT 'MPESA_TRANSACTIONS TABLE:' AS info;
DESCRIBE mpesa_transactions;

-- Show structure of deposits
SELECT 'DEPOSITS TABLE:' AS info;
DESCRIBE deposits;

-- Show structure of withdrawals
SELECT 'WITHDRAWALS TABLE:' AS info;
DESCRIBE withdrawals;

-- Show structure of member_accounts
SELECT 'MEMBER_ACCOUNTS TABLE:' AS info;
DESCRIBE member_accounts;

-- Show structure of loan_repayments
SELECT 'LOAN_REPAYMENTS TABLE:' AS info;
DESCRIBE loan_repayments;

-- Show structure of bulk_transaction_items
SELECT 'BULK_TRANSACTION_ITEMS TABLE:' AS info;
DESCRIBE bulk_transaction_items;

-- Show all foreign keys
SELECT 
    'FOREIGN KEYS:' AS info;

SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE
    REFERENCED_TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME IN (
        'loans',
        'loan_repayments',
        'loan_topup_requests',
        'topup_guarantors',
        'loan_guarantors',
        'bulk_transaction_items',
        'transactions',
        'mpesa_transactions'
    )
ORDER BY TABLE_NAME, CONSTRAINT_NAME;
