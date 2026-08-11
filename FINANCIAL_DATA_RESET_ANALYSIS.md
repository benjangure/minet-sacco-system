# Financial Data Reset - Analysis & Execution Plan

## Overview
This document outlines the safe deletion of incorrect financial data and preparation for importing correct data from Excel.

## Database Tables with Financial Data

### 1. **LOANS Table** (Primary financial data)
Contains:
- `amount` - Loan principal amount
- `interest_rate` - Interest rate percentage
- `term_months` - Loan term
- `monthly_repayment` - Monthly payment amount
- `total_interest` - Total interest to be paid
- `interest_collected` - Interest already collected
- `total_repayable` - Total amount to repay
- `outstanding_balance` - Current outstanding balance
- `original_principal` - Original loan amount
- `principal_repaid` - Principal amount repaid
- `interest_remaining` - Remaining interest
- `total_topup_amount` - Any top-up amounts
- `interest_collected_manual_override` - Flag for manual overrides
- `principal_repaid_manual_override` - Flag for principal overrides

### 2. **LOAN_REPAYMENTS Table** (Repayment records)
Contains:
- `amount` - Total repayment amount
- `interest_amount` - Interest portion
- `principal_amount` - Principal portion
- `repayment_date` - When payment was made
- `payment_method` - How payment was made
- `reference_number` - Payment reference

### 3. **TRANSACTIONS Table** (Account transactions)
Contains:
- `amount` - Transaction amount
- `transaction_type` - Type (deposit, withdrawal, loan_disbursement, loan_repayment, etc.)
- `transaction_date` - When transaction occurred
- `reference_number` - Transaction reference

### 4. **ACCOUNTS Table** (Member account balances)
Contains:
- `balance` - Current account balance
- `account_type` - Type of account (SAVINGS, SHARES, etc.)

### 5. **LOAN_TOPUP_HISTORY Table** (Top-up audit trail)
Contains:
- `topup_amount` - Top-up amount
- `outstanding_before_topup` - Balance before
- `outstanding_after_topup` - Balance after
- `principal_paid_before_topup` - Principal paid before

## CRITICAL CONSIDERATIONS

### ⚠️ Data Integrity
- **Foreign Key Constraints**: Some tables have foreign key relationships
- **Audit Trail**: Complete deletion will lose audit history
- **Member Relationships**: Must preserve member records

### ⚠️ What Will Be Affected
1. All loan financial calculations will be reset
2. All repayment records will be deleted
3. All financial transactions will be deleted
4. All account balances will be reset
5. Loan top-up history will be cleared

### ⚠️ What Will Be Preserved
1. Member records (personal information)
2. User accounts (staff/admin)
3. Loan product definitions
4. Guarantor relationships (structure, but financial amounts affected)
5. Loan application workflow status (can be reset if needed)

## SAFETY MEASURES

### 1. Full Database Backup
**MANDATORY** - Create complete backup before any deletion:
```sql
-- This will be done via mysqldump
mysqldump -u root -p sacco_db > backup_before_financial_reset_YYYYMMDD_HHMMSS.sql
```

### 2. Verification Queries
Before deletion, run these to understand current data:
```sql
-- Count records that will be deleted
SELECT 'loans' as table_name, COUNT(*) as record_count FROM loans
UNION ALL
SELECT 'loan_repayments', COUNT(*) FROM loan_repayments
UNION ALL
SELECT 'transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'loan_topup_history', COUNT(*) FROM loan_topup_history;

-- Total financial amounts
SELECT 
    SUM(amount) as total_loan_amount,
    SUM(total_interest) as total_interest,
    SUM(outstanding_balance) as total_outstanding
FROM loans;
```

### 3. Staged Deletion Approach
Delete in correct order to respect foreign key constraints:

**Order of Deletion:**
1. loan_repayments (depends on loans)
2. loan_topup_history (depends on loans)
3. transactions (standalone or depends on accounts)
4. Reset financial fields in loans table
5. Reset balances in accounts table

## EXECUTION PLAN

### Phase 1: Pre-Deletion (SAFETY CHECKS)
1. ✅ Create full database backup
2. ✅ Export current data to CSV for comparison
3. ✅ Verify backup integrity
4. ✅ Document current record counts

### Phase 2: Deletion (CAREFUL EXECUTION)
1. ✅ Disable foreign key checks temporarily
2. ✅ Delete loan_repayments
3. ✅ Delete loan_topup_history
4. ✅ Delete transactions
5. ✅ Reset loan financial fields
6. ✅ Reset account balances
7. ✅ Re-enable foreign key checks
8. ✅ Verify deletion

### Phase 3: Data Import (FROM EXCEL)
1. ✅ Validate Excel data format
2. ✅ Generate INSERT statements from Excel
3. ✅ Import loans data
4. ✅ Import repayment data
5. ✅ Recalculate account balances
6. ✅ Verify data integrity

### Phase 4: Verification (CONFIRM SUCCESS)
1. ✅ Compare record counts
2. ✅ Verify financial calculations
3. ✅ Test application functionality
4. ✅ Generate financial reports

## ROLLBACK PLAN

If anything goes wrong:
```sql
-- Drop the database
DROP DATABASE sacco_db;

-- Create new database
CREATE DATABASE sacco_db;

-- Restore from backup
mysql -u root -p sacco_db < backup_before_financial_reset_YYYYMMDD_HHMMSS.sql
```

## EXCEL FILE REQUIREMENTS

The Excel file should contain columns for:

### Loans Sheet:
- loan_number
- member_id (or member identifier)
- amount (principal)
- interest_rate
- term_months
- monthly_repayment
- total_interest
- total_repayable
- outstanding_balance
- application_date
- disbursement_date
- status

### Repayments Sheet (if applicable):
- loan_number (to link to loan)
- amount
- interest_amount
- principal_amount
- repayment_date
- payment_method
- reference_number

## NEXT STEPS

1. **PROVIDE EXCEL FILE** - Upload the Excel file with correct data
2. **REVIEW & APPROVE** - Review this plan and confirm you want to proceed
3. **CREATE BACKUP** - We'll create a full database backup
4. **EXECUTE SCRIPTS** - Run the deletion and import scripts
5. **VERIFY RESULTS** - Confirm data is correct

## SCRIPTS TO BE CREATED

1. `1_BACKUP_DATABASE.ps1` - Create full backup
2. `2_VERIFY_CURRENT_DATA.sql` - Document current state
3. `3_DELETE_FINANCIAL_DATA.sql` - Safe deletion script
4. `4_IMPORT_FROM_EXCEL.ps1` - Import correct data
5. `5_VERIFY_IMPORTED_DATA.sql` - Verify new data
6. `ROLLBACK_IF_NEEDED.sql` - Emergency rollback

---

## ⚠️ IMPORTANT WARNING

**THIS OPERATION WILL DELETE ALL FINANCIAL DATA!**

- Make absolutely sure you have the correct Excel file
- Ensure you have a recent database backup
- Test the import process on a copy first if possible
- Have the rollback plan ready

**Do not proceed without:**
1. ✅ Full database backup
2. ✅ Excel file with correct data
3. ✅ Understanding of what will be deleted
4. ✅ Approval from system administrator
