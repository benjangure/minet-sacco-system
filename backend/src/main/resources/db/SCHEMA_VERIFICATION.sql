-- ============================================================================
-- SCHEMA VERIFICATION - Check all table structures
-- ============================================================================

-- 1. Check MEMBERS table columns
DESCRIBE members;

-- 2. Check ACCOUNTS table columns
DESCRIBE accounts;

-- 3. Check LOAN_PRODUCTS table columns
DESCRIBE loan_products;

-- 4. Check LOANS table columns
DESCRIBE loans;

-- 5. Check GUARANTORS table columns
DESCRIBE guarantors;

-- 6. Check TRANSACTIONS table columns
DESCRIBE transactions;

-- ============================================================================
-- Additional checks
-- ============================================================================

-- Check if EMP019 and EMP020 exist
SELECT member_number, first_name, last_name FROM members WHERE member_number IN ('EMP019', 'EMP020');

-- Check existing loan numbers to avoid conflicts
SELECT DISTINCT loan_number FROM loans WHERE loan_number IS NOT NULL ORDER BY loan_number;

-- Check loan products available
SELECT id, name, interest_rate FROM loan_products;

-- Check if loan numbers LN-2026-00011 through LN-2026-00014 already exist
SELECT loan_number, status FROM loans WHERE loan_number IN ('LN-2026-00011', 'LN-2026-00012', 'LN-2026-00013', 'LN-2026-00014');
