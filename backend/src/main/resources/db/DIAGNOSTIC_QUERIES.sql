-- ============================================================================
-- DIAGNOSTIC QUERIES - Check existing data to avoid duplicates
-- ============================================================================
-- Run these queries to see what data already exists in the database

-- 1. Check all members
SELECT 'MEMBERS' as 'Data Type', COUNT(*) as 'Count' FROM members
UNION ALL
SELECT 'MEMBERS - Details', CONCAT(member_number, ' - ', first_name, ' ', last_name) FROM members
ORDER BY 1;

-- 2. Check all loan numbers (to avoid duplicates)
SELECT 'LOAN NUMBERS' as 'Data Type', COUNT(*) as 'Count' FROM loans
UNION ALL
SELECT 'LOAN NUMBERS - Details', CONCAT(loan_number, ' (', status, ')') FROM loans
ORDER BY 1;

-- 3. Check loan products
SELECT 'LOAN PRODUCTS' as 'Data Type', COUNT(*) as 'Count' FROM loan_products
UNION ALL
SELECT 'LOAN PRODUCTS - Details', CONCAT(name, ' - ', interest_rate, '% interest') FROM loan_products
ORDER BY 1;

-- 4. Check accounts
SELECT 'ACCOUNTS' as 'Data Type', COUNT(*) as 'Count' FROM accounts
UNION ALL
SELECT 'ACCOUNTS - Details', CONCAT('Member ID: ', member_id, ', Type: ', account_type, ', Balance: ', balance) FROM accounts
ORDER BY 1;

-- 5. Check transactions
SELECT 'TRANSACTIONS' as 'Data Type', COUNT(*) as 'Count' FROM transactions
UNION ALL
SELECT 'TRANSACTIONS - By Type', CONCAT(transaction_type, ': ', COUNT(*)) FROM transactions GROUP BY transaction_type
ORDER BY 1;

-- 6. Check guarantors
SELECT 'GUARANTORS' as 'Data Type', COUNT(*) as 'Count' FROM guarantors
UNION ALL
SELECT 'GUARANTORS - Details', CONCAT('Loan ID: ', loan_id, ', Member ID: ', member_id, ', Pledge: ', pledge_amount) FROM guarantors
ORDER BY 1;

-- 7. Check what loan numbers are already used (to avoid conflicts)
SELECT DISTINCT loan_number FROM loans ORDER BY loan_number;

-- 8. Check what member numbers exist
SELECT DISTINCT member_number FROM members ORDER BY member_number;

-- 9. Check EMP019 and EMP020 specifically
SELECT 'EMP019 Status' as 'Check', 
       CASE WHEN EXISTS(SELECT 1 FROM members WHERE member_number = 'EMP019') THEN 'EXISTS' ELSE 'NOT FOUND' END as 'Result'
UNION ALL
SELECT 'EMP020 Status', 
       CASE WHEN EXISTS(SELECT 1 FROM members WHERE member_number = 'EMP020') THEN 'EXISTS' ELSE 'NOT FOUND' END;

-- 10. Check if loans LN-2026-00011 through LN-2026-00014 already exist
SELECT loan_number, status, amount, member_id FROM loans 
WHERE loan_number IN ('LN-2026-00011', 'LN-2026-00012', 'LN-2026-00013', 'LN-2026-00014')
ORDER BY loan_number;

-- 11. Summary of all data
SELECT 'Members' as 'Entity', COUNT(*) as 'Count' FROM members
UNION ALL
SELECT 'Accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'Loans', COUNT(*) FROM loans
UNION ALL
SELECT 'Loan Products', COUNT(*) FROM loan_products
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'Guarantors', COUNT(*) FROM guarantors
ORDER BY 1;

-- 12. Check for any existing data for EMP019 and EMP020
SELECT 'EMP019 Accounts' as 'Check', COUNT(*) as 'Count' FROM accounts 
WHERE member_id = (SELECT id FROM members WHERE member_number = 'EMP019')
UNION ALL
SELECT 'EMP019 Loans', COUNT(*) FROM loans 
WHERE member_id = (SELECT id FROM members WHERE member_number = 'EMP019')
UNION ALL
SELECT 'EMP020 Accounts', COUNT(*) FROM accounts 
WHERE member_id = (SELECT id FROM members WHERE member_number = 'EMP020')
UNION ALL
SELECT 'EMP020 Loans', COUNT(*) FROM loans 
WHERE member_id = (SELECT id FROM members WHERE member_number = 'EMP020');
