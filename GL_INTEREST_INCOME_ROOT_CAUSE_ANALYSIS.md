# GL Loan Interest Income Bug - Root Cause Analysis

## Query Result (LIVE DATABASE)
```
id: 43
code: LOAN_INTEREST  
calculation_config: {"table":"transactions","field":"amount"}
```

## Problem Summary

**The GL Loan Interest Income account is reading ALL transactions instead of just INTEREST transactions because the calculation_config is missing the transactionType filter.**

---

## Root Cause Breakdown

### BUG #1: Account Code Mismatch (V117 Migration vs Live DB)
- **Migration file V117** defines account code: `INT_LOANS`
- **Live database** has account code: `LOAN_INTEREST`
- **Issue**: These should be the same account. Either:
  - The migration never ran (INT_LOANS never created), OR
  - Someone manually created LOAN_INTEREST, replacing INT_LOANS, OR
  - Both exist but only LOAN_INTEREST is being used

**Verification needed**: Run this query to confirm:
```sql
SELECT id, code, name FROM gl_accounts WHERE code IN ('INT_LOANS', 'LOAN_INTEREST');
```

### BUG #2: Missing transactionType Filter in LOAN_INTEREST Config
**Intended config** (from V117):
```json
{
  "table": "transactions",
  "field": "amount",
  "where": "INTEREST"
}
```

**Actual config** (in live db):
```json
{
  "table": "transactions",
  "field": "amount"
}
```

**Impact**: The query sums ALL transactions (deposits, withdrawals, disbursements, repayments, interest, bank charges) instead of just INTEREST transactions.

**How GLCalculationService.extractTransactionType() would handle this**:
1. Checks for `transactionType` field → **NOT FOUND**
2. Checks for `where` field → **NOT FOUND** (missing entirely)
3. Returns `null`
4. Query sums ALL amounts without filtering

### BUG #3: Other Malformed WHERE Clauses in V117

The migration file itself has incomplete/malformed WHERE clauses:

| Account | Current Where | Issue |
|---------|---------------|-------|
| MEMBER_DEPOSITS | `"SAVINGS"` | Not valid SQL - missing table/column context |
| MEMBER_SHARES | `"SHARES"` | Not valid SQL - missing table/column context |
| CBA_CALL_DEPOSITS | `"SAVINGS account"` | Not valid SQL - incomplete string |
| CBA_CURRENT | `"SAVINGS account"` | Not valid SQL - incomplete string |

These would likely fail or be silently ignored by GLCalculationService.

---

## Transaction.TransactionType Enum

From `Transaction.java` (line 40):
```java
public enum TransactionType { 
  DEPOSIT, 
  WITHDRAWAL, 
  LOAN_DISBURSEMENT, 
  LOAN_REPAYMENT, 
  INTEREST, 
  LOAN_DEFAULT_DEBIT 
}
```

**For LOAN_INTEREST account, the correct transactionType is: `INTEREST`**

---

## Why Interest Transactions May Not Exist

Current code paths:

1. **LoanService.makeRepayment()** - Main repayment path
   - Creates LoanRepayment record ✓
   - **DOES NOT create Transaction record with transactionType=INTEREST** ✗
   
2. **BulkProcessingService** - Bulk repayments
   - Calls LoanService.makeRepayment()
   - **DOES NOT create Transaction records** ✗

3. **LoanRepaymentService** (older method, likely unused)
   - Creates Transaction records with transactionType=INTEREST ✓
   - **Appears abandoned**

**Result**: No INTEREST transactions exist in database → GL account sums zero anyway

---

## Fix Required

### Step 1: Fix LOAN_INTEREST Config (Database)
Update the calculation_config to include transactionType:

```sql
UPDATE gl_accounts 
SET calculation_config = '{"table":"transactions","field":"amount","transactionType":"INTEREST"}'
WHERE code = 'LOAN_INTEREST';
```

### Step 2: Ensure INT_LOANS doesn't Exist (or use it instead)
```sql
SELECT id, code FROM gl_accounts WHERE code IN ('INT_LOANS', 'LOAN_INTEREST');
```

If both exist, keep only LOAN_INTEREST. If only INT_LOANS exists, rename it to LOAN_INTEREST.

### Step 3: Add Transaction Creation in LoanService.makeRepayment()
When interest is collected (interestAmount > 0), create a Transaction record:

```java
if (loanRepayment.getInterestAmount() != null && loanRepayment.getInterestAmount() > 0) {
    Transaction interestTxn = new Transaction();
    interestTxn.setAccount(loan.getMember().getAccount());
    interestTxn.setType(Transaction.TransactionType.INTEREST);
    interestTxn.setAmount(loanRepayment.getInterestAmount());
    interestTxn.setDescription("Interest on " + loan.getId());
    interestTxn.setTransactionDate(new Date());
    transactionRepository.save(interestTxn);
}
```

### Step 4: Backfill Missing INTEREST Transactions
Find all LoanRepayments with interest but no corresponding transaction:

```sql
SELECT lr.id, lr.loan_id, lr.interest_amount, lr.repayment_date
FROM loan_repayment lr
WHERE lr.interest_amount > 0
AND NOT EXISTS (
  SELECT 1 FROM transaction t 
  WHERE t.account_id = (SELECT account_id FROM member WHERE id = (SELECT member_id FROM loan WHERE id = lr.loan_id))
  AND t.transaction_type = 'INTEREST'
  AND DATE(t.transaction_date) = DATE(lr.repayment_date)
);
```

### Step 5: Fix Other Malformed Configs in V117
The WHERE clauses for MEMBER_DEPOSITS, MEMBER_SHARES, CBA accounts need proper SQL.

These should reference the actual transaction types or account types, not incomplete strings.

---

## Verification After Fix

After applying fixes, the GL account should:
1. Read only transactions with `type = 'INTEREST'`
2. Sum the `amount` field from those transactions
3. Show non-zero balance if INTEREST transactions have been created during repayments

**Test query** (should show INTEREST transactions):
```sql
SELECT t.id, t.type, t.amount, t.transaction_date 
FROM transaction t 
WHERE t.transaction_type = 'INTEREST'
LIMIT 10;
```

