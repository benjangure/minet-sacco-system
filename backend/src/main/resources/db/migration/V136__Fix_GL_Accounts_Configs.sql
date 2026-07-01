-- Fix GL Accounts Configuration Issues
-- This migration corrects malformed calculation_config values and account naming
-- Date: 2026-06-29

-- 1. Remove INT_LOANS if it exists (old account code from V117)
DELETE FROM gl_accounts WHERE code = 'INT_LOANS';

-- 2. Update LOAN_INTEREST with correct config including transactionType
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','transactions','field','amount','transactionType','INTEREST')
WHERE code = 'LOAN_INTEREST' AND account_type = 'REVENUE';

-- 3. Fix MEMBER_DEPOSITS config (incomplete WHERE clause)
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','accounts','field','balance','where','account_type = ''SAVINGS''')
WHERE code = 'MEMBER_DEPOSITS';

-- 4. Fix MEMBER_SHARES config (incomplete WHERE clause)
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','accounts','field','balance','where','account_type = ''SHARES''')
WHERE code = 'MEMBER_SHARES';

-- 5. Fix CBA_CALL_DEPOSITS config (malformed WHERE clause)
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','accounts','field','balance','where','account_type = ''SAVINGS''')
WHERE code = 'CBA_CALL_DEPOSITS';

-- 6. Fix CBA_CURRENT config (malformed WHERE clause)
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','accounts','field','balance','where','account_type = ''CURRENT''')
WHERE code = 'CBA_CURRENT';

-- 7. Verify ENTRANCE_FEES and BANK_CHARGES have correct transactionType
UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','transactions','field','amount','transactionType','ENTRANCE_FEE')
WHERE code = 'ENTRANCE_FEES' AND account_type = 'REVENUE';

UPDATE gl_accounts 
SET calculation_config = JSON_OBJECT('table','transactions','field','amount','transactionType','BANK_CHARGE')
WHERE code = 'BANK_CHARGES' AND account_type = 'EXPENSE';
