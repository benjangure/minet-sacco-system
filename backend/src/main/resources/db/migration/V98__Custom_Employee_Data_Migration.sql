-- V98__Custom_Employee_Data_Migration.sql
-- SACCO Data Migration: Realistic test data for reports
-- Includes: Members, Accounts, Loans, Guarantors, Transactions
-- Purpose: Digitize existing SACCO data for accurate report testing

-- ============================================================================
-- MEMBERS: 2 employees for test data (EMP019 and EMP020)
-- NOTE: These members are created here, but their user accounts (for login)
-- are created by the Java-based migration V103 which properly encodes passwords
-- ============================================================================
INSERT IGNORE INTO members (member_number, employee_id, first_name, last_name, email, phone, national_id, date_of_birth,
                            employment_status, employer, department, status, is_legacy_member, 
                            bank_name, bank_account_number, bank_branch,
                            next_of_kin_name, next_of_kin_phone, next_of_kin_relationship,
                            approved_at, created_at, updated_at)
VALUES 
('EMP019', 'EMP019', 'Samuel', 'Kipchoge', 'samuel.kipchoge@company.com', '0712345678', '11111111', '1985-06-15',
 'PERMANENT', 'Minet Insurance', 'Finance', 'ACTIVE', TRUE,
 'Equity Bank', '0123456789', 'Nairobi CBD',
 'Mary Kipchoge', '0712345679', 'Spouse',
 NOW(), '2023-06-01', NOW()),
('EMP020', 'EMP020', 'Grace', 'Omondi', 'grace.omondi@company.com', '0723456789', '87600321', '1990-03-22',
 'PERMANENT', 'Minet Insurance', 'HR', 'ACTIVE', TRUE,
 'KCB Bank', '9876543210', 'Westlands',
 'John Omondi', '0723456790', 'Brother',
 NOW(), '2023-06-15', NOW());

-- ============================================================================
-- ACCOUNTS: Savings accounts for EMP019 and EMP020
-- ============================================================================
INSERT INTO accounts (member_id, account_type, balance, frozen_savings, created_at, updated_at)
SELECT id, 'SAVINGS', 250000, 0, NOW(), NOW() FROM members WHERE member_number = 'EMP019'
ON DUPLICATE KEY UPDATE balance = 250000, updated_at = NOW();

INSERT INTO accounts (member_id, account_type, balance, frozen_savings, created_at, updated_at)
SELECT id, 'SAVINGS', 200000, 0, NOW(), NOW() FROM members WHERE member_number = 'EMP020'
ON DUPLICATE KEY UPDATE balance = 200000, updated_at = NOW();

-- ============================================================================
-- LOAN PRODUCTS (needed for loans)
-- ============================================================================
INSERT INTO loan_products (name, description, interest_rate, max_amount, min_amount, min_term_months, max_term_months, is_active, created_at)
VALUES 
('Standard Loan', 'Regular member loan', 10, 500000, 10000, 6, 24, TRUE, NOW()),
('Emergency Loan', 'Quick emergency loan', 12, 200000, 5000, 3, 12, TRUE, NOW())
ON DUPLICATE KEY UPDATE created_at = NOW();

-- ============================================================================
-- LOANS: Various statuses for testing reports (using EMP019 and EMP020)
-- ============================================================================
-- Loan 1: DISBURSED (active loan) - EMP019
-- Amount: 100,000, Rate: 10%, Term: 12 months
-- Interest: 100,000 * 0.10 * 1 = 10,000
-- Total Repayable: 110,000
-- Monthly: 9,166.67
INSERT INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status, 
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose, 
                   application_date, approval_date, disbursement_date)
SELECT m.id, lp.id, 'LN-2026-00011', 100000, 10, 12, 'DISBURSED',
       9166.67, 10000, 110000,
       100000, 100000, 85000, 'Business expansion',
       '2026-01-10', '2026-01-20', '2026-01-25'
FROM members m, loan_products lp WHERE m.member_number = 'EMP019' AND lp.name = 'Standard Loan'
ON DUPLICATE KEY UPDATE outstanding_balance = 85000;

-- Loan 2: REPAID (completed loan) - EMP020
-- Amount: 75,000, Rate: 10%, Term: 12 months
-- Interest: 75,000 * 0.10 * 1 = 7,500
-- Total Repayable: 82,500
-- Monthly: 6,875
INSERT INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status,
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose,
                   application_date, approval_date, disbursement_date)
SELECT m.id, lp.id, 'LN-2026-00012', 75000, 10, 12, 'REPAID',
       6875, 7500, 82500,
       75000, 75000, 0, 'Home renovation',
       '2025-06-10', '2025-06-20', '2025-06-25'
FROM members m, loan_products lp WHERE m.member_number = 'EMP020' AND lp.name = 'Standard Loan'
ON DUPLICATE KEY UPDATE outstanding_balance = 0;

-- Loan 3: DEFAULTED (overdue loan) - EMP019
-- Amount: 150,000, Rate: 10%, Term: 12 months
-- Interest: 150,000 * 0.10 * 1 = 15,000
-- Total Repayable: 165,000
-- Monthly: 13,750
INSERT INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status,
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose,
                   application_date, approval_date, disbursement_date)
SELECT m.id, lp.id, 'LN-2026-00013', 150000, 10, 12, 'DEFAULTED',
       13750, 15000, 165000,
       150000, 150000, 45000, 'Equipment purchase',
       '2025-08-10', '2025-08-20', '2025-08-25'
FROM members m, loan_products lp WHERE m.member_number = 'EMP019' AND lp.name = 'Standard Loan'
ON DUPLICATE KEY UPDATE outstanding_balance = 45000;

-- Loan 4: APPROVED (pending disbursement) - EMP020
-- Amount: 120,000, Rate: 12%, Term: 12 months
-- Interest: 120,000 * 0.12 * 1 = 14,400
-- Total Repayable: 134,400
-- Monthly: 11,200
INSERT INTO loans (member_id, loan_product_id, loan_number, amount, interest_rate, term_months, status,
                   monthly_repayment, total_interest, total_repayable,
                   original_principal, original_amount, outstanding_balance, purpose,
                   application_date, approval_date)
SELECT m.id, lp.id, 'LN-2026-00014', 120000, 12, 12, 'APPROVED',
       11200, 14400, 134400,
       120000, 120000, 120000, 'Vehicle purchase',
       '2026-04-10', '2026-04-20'
FROM members m, loan_products lp WHERE m.member_number = 'EMP020' AND lp.name = 'Emergency Loan'
ON DUPLICATE KEY UPDATE outstanding_balance = 120000;

-- ============================================================================
-- GUARANTORS: Link members as guarantors to loans (EMP019 and EMP020 only)
-- ============================================================================
-- Guarantors for Loan 1 (EMP019's loan - 100,000)
INSERT INTO guarantors (loan_id, member_id, pledge_amount, status)
SELECT l.id, m.id, 50000, 'ACTIVE'
FROM loans l, members m WHERE l.loan_number = 'LN-2026-00011' AND m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE pledge_amount = 50000;

-- Guarantors for Loan 3 (EMP019's loan - 150,000)
INSERT INTO guarantors (loan_id, member_id, pledge_amount, status)
SELECT l.id, m.id, 75000, 'ACTIVE'
FROM loans l, members m WHERE l.loan_number = 'LN-2026-00013' AND m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE pledge_amount = 75000;

-- ============================================================================
-- TRANSACTIONS: Deposits, withdrawals, loan disbursements, repayments, interest
-- ============================================================================

-- ============================================================================
-- DEPOSITS: Monthly contributions from EMP019 and EMP020
-- ============================================================================
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 50000, 'Monthly contribution - January', '2026-01-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 50000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 50000, 'Monthly contribution - February', '2026-02-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 50000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 40000, 'Monthly contribution - January', '2026-01-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 40000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'DEPOSIT', 40000, 'Monthly contribution - February', '2026-02-05'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 40000;

-- ============================================================================
-- LOAN DISBURSEMENTS
-- ============================================================================
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_DISBURSEMENT', 100000, 'Loan LN-2026-00011 disbursed', '2026-01-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 100000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_DISBURSEMENT', 75000, 'Loan LN-2026-00012 disbursed', '2025-06-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 75000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_DISBURSEMENT', 150000, 'Loan LN-2026-00013 disbursed', '2025-08-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 150000;

-- ============================================================================
-- LOAN REPAYMENTS
-- ============================================================================
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 1', '2025-07-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 2', '2025-08-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 3', '2025-09-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 4', '2025-10-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 5', '2025-11-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 6875, 'Loan LN-2026-00012 repayment - Month 6', '2025-12-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 6875;

-- Partial repayments for active loans
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 9166.67, 'Loan LN-2026-00011 repayment - Month 1', '2026-02-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 9166.67;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'LOAN_REPAYMENT', 9166.67, 'Loan LN-2026-00011 repayment - Month 2', '2026-03-25'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 9166.67;

-- ============================================================================
-- WITHDRAWALS
-- ============================================================================
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'WITHDRAWAL', 30000, 'Member withdrawal', '2026-01-15'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 30000;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'WITHDRAWAL', 25000, 'Member withdrawal', '2026-02-20'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP020'
ON DUPLICATE KEY UPDATE amount = 25000;

-- ============================================================================
-- INTEREST TRANSACTIONS (for Profit & Loss report)
-- ============================================================================
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'INTEREST', 833.33, 'Interest on Loan LN-2026-00011 - Month 1', '2026-02-01'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 833.33;

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date)
SELECT a.id, 'INTEREST', 833.33, 'Interest on Loan LN-2026-00011 - Month 2', '2026-03-01'
FROM accounts a JOIN members m ON a.member_id = m.id WHERE m.member_number = 'EMP019'
ON DUPLICATE KEY UPDATE amount = 833.33;
