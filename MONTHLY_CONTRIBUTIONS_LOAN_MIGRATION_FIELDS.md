# Monthly Contributions & Loan Migration Fields Reference

## PART 1: MONTHLY CONTRIBUTION TRACKING FIELDS

### Overview
Monthly contributions are tracked through `BulkTransactionItem` entity, which is part of the `BulkBatch` system. Each item represents one member's monthly transactions for a specific batch/upload.

### Database Table: `bulk_transaction_items`

#### Core Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `batch_id` | BIGINT | Foreign key to batch this item belongs to |
| `row_number` | INT | Row number in the uploaded file |
| `member_number` | VARCHAR(50) | Member identifier |
| `member_id` | BIGINT | Foreign key to member |

#### Savings & Shares (Member Contributions)
| Field | Type | Description |
|-------|------|-------------|
| `savings_amount` | DECIMAL(15,2) | Regular savings contribution for the month |
| `shares_amount` | DECIMAL(15,2) | Shares/equity contribution for the month |
| `savings_transaction_id` | BIGINT | Link to created savings transaction |
| `shares_transaction_id` | BIGINT | Link to created shares transaction |

#### Loan Repayment (Main Focus for Reducing Balance)
| Field | Type | Description |
|-------|------|-------------|
| `loan_number` | VARCHAR(50) | Which loan is being repaid |
| `loan_id` | BIGINT | Foreign key to loan |
| `loan_repayment_amount` | DECIMAL(15,2) | **TOTAL amount being repaid** |
| `loan_repayment_principal_amount` | DECIMAL(15,2) | **Principal portion of repayment** ← NEW for reducing balance |
| `loan_repayment_interest_amount` | DECIMAL(15,2) | **Interest portion of repayment** ← NEW for reducing balance |
| `loan_repayment_payment_method` | VARCHAR(20) | Payment method (SALARY_DEDUCTION, CASH, etc.) |
| `loan_repayment_reference_number` | VARCHAR(50) | Reference/check number |
| `loan_repayment_id` | BIGINT | Link to created LoanRepayment record |

#### Fund Contributions (Additional)
| Field | Type | Description |
|-------|------|-------------|
| `benevolent_fund_amount` | DECIMAL(15,2) | Contribution to benevolent fund |
| `development_fund_amount` | DECIMAL(15,2) | Contribution to development fund |
| `school_fees_amount` | DECIMAL(15,2) | Contribution to school fees fund |
| `holiday_fund_amount` | DECIMAL(15,2) | Contribution to holiday fund |
| `emergency_fund_amount` | DECIMAL(15,2) | Contribution to emergency fund |

#### Processing Status
| Field | Type | Description |
|-------|------|-------------|
| `status` | VARCHAR(20) | PENDING, PROCESSING, COMPLETED, ERROR |
| `error_message` | VARCHAR(2000) | Error details if processing failed |
| `processed_at` | DATETIME | When this item was processed |

---

## PART 2: LOAN MIGRATION FIELDS

### Overview
Loan migration is used to import historical loan data from the old system. Handled through `LoanMigrationItem` entity, which represents a single loan record being migrated.

### Database Table: `loan_migration_items`

#### Batch Reference
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `batch_id` | BIGINT | Foreign key to migration batch |
| `row_number` | INT | Row number in uploaded Excel file |

#### Borrower Information
| Field | Type | Description |
|-------|------|-------------|
| `employee_id` | VARCHAR | Employee ID / Member identifier |

#### Loan Product & Terms
| Field | Type | Description |
|-------|------|-------------|
| `loan_number` | VARCHAR(50) | Original loan number from old system |
| `loan_product_name` | VARCHAR | Name of loan product (e.g., "Staff Loan", "Vehicle Loan") |
| `principal_amount` | DECIMAL(15,2) | Original principal amount borrowed |
| `term_months` | INT | Loan term in months |
| `interest_rate` | DECIMAL(5,2) | Interest rate (annual %) - optional, uses product default if null |
| `purpose` | VARCHAR(500) | Loan purpose/description |

#### Loan Status & Dates
| Field | Type | Description |
|-------|------|-------------|
| `loan_status` | VARCHAR(20) | DISBURSED, REPAID, or DEFAULTED |
| `disbursement_date` | DATE | When loan was originally disbursed |
| `outstanding_balance` | DECIMAL(15,2) | **Current amount still owed** (from old system) |

#### Guarantors (Up to 6 Allowed)
| Field Pair | Type | Description |
|-----------|------|-------------|
| `guarantor1_employee_id` + `guarantor1_pledge_amount` | VARCHAR + DECIMAL | 1st guarantor and pledge amount |
| `guarantor2_employee_id` + `guarantor2_pledge_amount` | VARCHAR + DECIMAL | 2nd guarantor and pledge amount |
| `guarantor3_employee_id` + `guarantor3_pledge_amount` | VARCHAR + DECIMAL | 3rd guarantor and pledge amount |
| `guarantor4_employee_id` + `guarantor4_pledge_amount` | VARCHAR + DECIMAL | 4th guarantor and pledge amount |
| `guarantor5_employee_id` + `guarantor5_pledge_amount` | VARCHAR + DECIMAL | 5th guarantor and pledge amount |
| `guarantor6_employee_id` + `guarantor6_pledge_amount` | VARCHAR + DECIMAL | 6th guarantor and pledge amount |

#### Guarantorship Type
| Field | Type | Description |
|-------|------|-------------|
| `guarantorship_type` | VARCHAR(10) | NORMAL or SELF (member is self-guarantor) |

#### Calculated Fields (Set During Migration)
| Field | Type | Description |
|-------|------|-------------|
| `total_interest` | DECIMAL(15,2) | Calculated upfront interest (simple interest) |
| `total_repayable` | DECIMAL(15,2) | Principal + total interest |
| `monthly_repayment` | DECIMAL(15,2) | Monthly payment amount |

#### Processing Result
| Field | Type | Description |
|-------|------|-------------|
| `loan_id` | BIGINT | Foreign key to created Loan record (if successful) |
| `status` | VARCHAR(20) | PENDING, SUCCESS, ERROR |
| `error_message` | VARCHAR(1000) | Error details if migration failed |
| `processed_at` | DATETIME | When item was processed |

---

## SIDE-BY-SIDE COMPARISON

### For Loan Repayment Tracking

**Monthly Contribution Recording (BulkTransactionItem)**
```
Monthly batch upload:
├─ member_number: "EMP001"
├─ loan_number: "L-001"
├─ loan_repayment_amount: 9,583.33 (TOTAL)
├─ loan_repayment_principal_amount: 9,000.00
├─ loan_repayment_interest_amount: 583.33
├─ loan_repayment_payment_method: "SALARY_DEDUCTION"
└─ status: COMPLETED
```

**Loan Migration (LoanMigrationItem)**
```
Historical loan import:
├─ employee_id: "EMP001"
├─ loan_number: "L-001" (from old system)
├─ principal_amount: 100,000.00
├─ outstanding_balance: 105,416.67 (what's left to pay)
├─ total_interest: 15,000.00 (pre-calculated upfront)
├─ total_repayable: 115,000.00
└─ status: SUCCESS
```

---

## KEY INSIGHTS FOR REDUCING BALANCE IMPLEMENTATION

### 1. Monthly Contribution Already Has Split Fields
✅ **Good news:** The system already captures:
- `loan_repayment_principal_amount` - Principal portion
- `loan_repayment_interest_amount` - Interest portion
- `loan_repayment_amount` - Total

This means the infrastructure is ready for recording reducing balance interest!

### 2. Loan Migration Pre-Calculates Interest
❌ **Issue:** Migrated loans have `total_interest` pre-calculated using simple interest
- This was fine for old loans (already accrued)
- For future repayments, should be calculated monthly based on reducing balance

### 3. Outstanding Balance Is the Key
✅ **Critical Field:** `loan_migration_items.outstanding_balance`
- This is what member owes at migration time
- Used as starting point for future reducing balance calculations
- As repayments are made, this decreases

### 4. Loan Entity Tracks These Too
In the `loans` table:
- `outstanding_balance` - Amount still owed (reduces with each payment)
- `interest_remaining` - Interest portion still uncollected (reduces with each payment)
- `total_interest` - Original calculated interest (doesn't change, historical)

---

## MONTHLY CONTRIBUTION WORKFLOW FOR REDUCING BALANCE

### Current Flow (from BulkTransactionItem perspective)

1. **Upload bulk contribution file**
   ```
   Employee ID | Loan# | Repayment | Principal | Interest
   EMP001      | L-001 | 9,583.33  | 9,000.00  | 583.33
   ```

2. **System processes each row:**
   - Finds member and loan
   - Validates split (principal + interest = total)
   - Creates LoanRepayment record with both amounts
   - Updates loan outstanding balance
   - Creates BulkTransactionItem with status COMPLETED

3. **Loan gets updated:**
   ```
   Before:
   ├─ Outstanding: 115,000
   ├─ Interest Remaining: 15,000
   
   After:
   ├─ Outstanding: 105,416.67 (115,000 - 9,583.33)
   ├─ Interest Remaining: 14,416.67 (15,000 - 583.33)
   ```

4. **Next month's interest calculation** (for guidance)
   ```
   Days since last payment: ~30
   Current outstanding: 105,416.67
   Expected interest = 105,416.67 × (15% / 365) × 30
                    = 1,297.97 KES
   ```

---

## MIGRATION HISTORY

### What Gets Migrated?
From `loan_migration_items`:
- Principal amount from old system
- Outstanding balance from old system
- Interest pre-calculated based on old system's method
- Guarantors and their pledges
- Loan status (DISBURSED, REPAID, DEFAULTED)
- Disbursement date

### What Happens During Migration?
`LoanMigrationService.processItem()` does:

1. **Validates the item** - checks member exists, product exists, amounts valid
2. **Creates Loan record:**
   ```
   loan.amount = principal_amount
   loan.interestRate = (from product or specified)
   loan.termMonths = term_months
   loan.outstandingBalance = outstanding_balance (from old system)
   loan.totalInterest = (pre-calculated)
   loan.status = DISBURSED (for active migrated loans)
   loan.migrationStatus = "MIGRATED" (to distinguish)
   ```

3. **Creates LOAN_DISBURSEMENT transaction** for audit trail
4. **Creates guarantor relationships** for each guarantor
5. **Sets migration_status flag** on loan

### Key Migration Detail
```
loan.outstanding_balance = item.outstanding_balance
```
This is NOT recalculated. It's the actual amount owed from the old system.

---

## REDUCING BALANCE IMPLEMENTATION CHECKLIST

Based on these fields, here's what needs to happen:

### For Monthly Contributions
- ✅ Already tracking principal/interest split in `BulkTransactionItem`
- ✅ Need to enhance UI to suggest split based on reducing balance
- ⚠️ Backend should calculate expected interest before processing batch

### For Loan Migration
- ❌ Currently pre-calculates total interest using simple interest
- ⚠️ Migration is OK (historical data), but going forward for this loan:
  - Store `original_principal` (for reference)
  - Store `outstanding_balance` (starting point)
  - Calculate future interest monthly, not upfront

### For New Loans
- ❌ Current system still pre-calculates `totalInterest`
- ⚠️ Should be changed to:
  - Store only: principal, rate, term, disbursement date
  - Calculate interest monthly at repayment time
  - Don't pre-calculate total interest

### For UI/Reporting
- Need "Expected Interest Calculator" service
- Need to show guidance in repayment UI
- Need to track "Last Payment Date" on loans for calculation

---

## SQL Quick Reference

### View Monthly Contributions for a Loan
```sql
SELECT 
    b.batch_number,
    b.uploaded_at,
    m.member_number,
    bti.loan_number,
    bti.loan_repayment_amount,
    bti.loan_repayment_principal_amount,
    bti.loan_repayment_interest_amount,
    bti.status
FROM bulk_transaction_items bti
JOIN bulk_batches b ON bti.batch_id = b.id
JOIN members m ON bti.member_id = m.id
WHERE bti.loan_id = ?
ORDER BY b.uploaded_at DESC;
```

### View Migrated Loans
```sql
SELECT 
    loan_number,
    principal_amount,
    outstanding_balance,
    total_interest,
    total_repayable,
    loan_status,
    disbursement_date,
    guarantor1_employee_id,
    guarantor1_pledge_amount
FROM loan_migration_items
WHERE status = 'SUCCESS'
ORDER BY id DESC;
```

### See Both Paths (Migration vs. Monthly)
```sql
-- Loan from migration
SELECT 
    'MIGRATED' as source,
    l.loan_number,
    l.amount as principal,
    l.outstanding_balance,
    l.total_interest,
    l.migration_status,
    l.disbursement_date
FROM loans l
WHERE l.migration_status = 'MIGRATED'
  AND l.id = ?

UNION

-- Current repayments (monthly)
SELECT 
    'MONTHLY' as source,
    l.loan_number,
    lr.principal_amount,
    l.outstanding_balance,
    lr.interest_amount,
    lr.created_at,
    lr.payment_date
FROM loan_repayments lr
JOIN loans l ON lr.loan_id = l.id
WHERE l.id = ?
ORDER BY 6 DESC;
```

---

## Summary

| Aspect | Monthly Contribution | Loan Migration |
|--------|---------------------|-----------------|
| **Table** | bulk_transaction_items | loan_migration_items |
| **Purpose** | Track ongoing monthly payments | Import historical loans |
| **Interest Split** | Already has columns | Calculates upfront |
| **Principal Tracking** | Has separate field ✅ | Has field ✅ |
| **Reducing Balance Ready** | YES - structure exists | Needs refinement for future |
| **UI Ready** | Partially - no guidance yet | N/A |
| **Calculation Method** | Manual entry (needs guidance) | Auto-calculated upfront |
| **Key Issue** | Need to show expected interest | Pre-calculates total interest |
