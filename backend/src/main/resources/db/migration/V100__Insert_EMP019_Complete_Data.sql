-- V100__Insert_EMP019_Complete_Data.sql
-- Insert EMP019 member with complete profile and all related data
-- Includes: employee_id, date_of_birth, bank details, next of kin, accounts, loans, guarantors, transactions

-- ============================================================================
-- INSERT EMP019 MEMBER WITH COMPLETE PROFILE
-- ============================================================================
INSERT IGNORE INTO members (member_number, employee_id, first_name, last_name, email, phone, national_id, date_of_birth,
                            employment_status, employer, department, status, is_legacy_member, 
                            bank_name, bank_account_number, bank_branch, 
                            next_of_kin_name, next_of_kin_phone, next_of_kin_relationship, 
                            approved_at, created_at, updated_at)
VALUES ('EMP019', 'EMP019', 'Samuel', 'Kipchoge', 'samuel.kipchoge@company.com', '0712345678', '11111111', '1985-06-15',
        'PERMANENT', 'Minet Insurance', 'Finance', 'ACTIVE', TRUE,
        'Equity Bank', '0123456789', 'Nairobi CBD',
        'Mary Kipchoge', '0712345679', 'Spouse',
        NOW(), '2023-06-01', NOW());

-- ============================================================================
-- CREATE SAVINGS ACCOUNT FOR EMP019
-- ============================================================================
INSERT IGNORE INTO accounts (member_id, account_type, balance, frozen_savings, created_at, updated_at)
SELECT id, 'SAVINGS', 250000, 0, NOW(), NOW() FROM members WHERE member_number = 'EMP019';

-- ============================================================================
-- CREATE LOANS FOR EMP019
-- ============================================================================
-- Loan 1: DISBURSED (active loan) - EMP019
INSERT IGNORE INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status, 
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose, 
                   application_date, approval_date, disbursement_date)
SELECT m.id, lp.id, 'LN-2026-00011', 100000, 10, 12, 'DISBURSED',
       9166.67, 10000, 110000,
       100000, 100000, 85000, 'Business expansion',
       '2026-01-10', '2026-01-20', '2026-01-25'
FROM members m, loan_products lp WHERE m.member_number = 'EMP019' AND lp.name = 'Standard Loan';

-- Loan 3: DEFAULTED (overdue loan) - EMP019
INSERT IGNORE INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status,
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose,
                   application_date, approval_date, disbursement_date)
SELECT m.id, lp.id, 'LN-2026-00013', 150000, 10, 12, 'DEFAULTED',
       13750, 15000, 165000,
       150000, 150000, 45000, 'Equipment purchase',
       '2025-08-10', '2025-08-20', '2025-08-25'
FROM members m, loan_products lp WHERE m.member_number = 'EMP019' AND lp.name = 'Standard Loan';

-- ============================================================================
-- CREATE GUARANTOR RELATIONSHIPS (EMP020 guarantees EMP019's loans)
-- ============================================================================
INSERT IGNORE INTO guarantors (loan_id, member_id, pledge_amount, status)
SELECT l.id, m.id, 50000, 'ACTIVE'
FROM loans l, members m WHERE l.loan_number = 'LN-2026-00011' AND m.member_number = 'EMP020';

INSERT IGNORE INTO guarantors (loan_id, member_id, pledge_amount, status)
SELECT l.id, m.id, 75000, 'ACTIVE'
FROM loans l, members m WHERE l.loan_number = 'LN-2026-00013' AND m.member_number = 'EMP020';

-- ============================================================================
-- CREATE TRANSACTIONS FOR EMP019
-- ============================================================================
-- Deposits
INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 50000, 'Monthly contribution - January', '2026-01-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 50000, 'Monthly contribution - February', '2026-02-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

-- Loan Disbursements
INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_DISBURSEMENT', 100000, 'Loan LN-2026-00011 disbursed', '2026-01-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_DISBURSEMENT', 150000, 'Loan LN-2026-00013 disbursed', '2025-08-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

-- Loan Repayments
INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 9166.67, 'Loan LN-2026-00011 repayment - Month 1', '2026-02-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 9166.67, 'Loan LN-2026-00011 repayment - Month 2', '2026-03-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

-- Withdrawals
INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'WITHDRAWAL', 30000, 'Member withdrawal', '2026-01-15'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

-- Interest
INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'INTEREST', 833.33, 'Interest on Loan LN-2026-00011 - Month 1', '2026-02-01'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';

INSERT IGNORE INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'INTEREST', 833.33, 'Interest on Loan LN-2026-00011 - Month 2', '2026-03-01'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019';
