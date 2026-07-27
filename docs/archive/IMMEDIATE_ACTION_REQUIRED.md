# IMMEDIATE ACTION REQUIRED - Fix Outstanding Balance

## Problem
The repayment display shows **-40% and KES -80,000 repaid** instead of **0% and KES 0**.

**Root Cause:** Database field `outstanding_balance` is 360,000 when it should be 280,000.

## Solution - Two Options

### OPTION 1: Direct Database Fix (FASTEST - Do This Now)
Run this SQL directly in your MySQL database:

```sql
UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE (status = 'DISBURSED' OR status = 'REPAID') 
AND outstanding_balance != total_repayable;
```

**Steps:**
1. Open MySQL Workbench or phpMyAdmin
2. Connect to `sacco_db` database
3. Run the SQL above
4. Refresh the frontend - the display should now show 0% and KES 0

### OPTION 2: Restart Backend (Automatic Fix)
1. Stop the backend (Ctrl+C in IntelliJ)
2. Wait 5 seconds
3. Restart the backend
4. Flyway will execute V90 migration automatically
5. Refresh the frontend

## Expected Result After Fix
For loan ID 13 (Stanley Mwangi):
- **Disbursed:** KES 200,000 ✓
- **Repaid:** KES 0 ✓ (was -80,000)
- **Outstanding:** KES 280,000 ✓
- **Repayment Status:** 0% ✓ (was -40%)

## Why This Happened
When the loan was disbursed, the code should have set `outstanding_balance = totalRepayable` (280,000), but it was set to 360,000 instead. This caused the calculation:
- Repaid = totalRepayable - outstandingBalance = 280,000 - 360,000 = -80,000 ✗

After the fix:
- Repaid = totalRepayable - outstandingBalance = 280,000 - 280,000 = 0 ✓

## Files Changed
- `backend/src/main/resources/db/migration/V89__Fix_outstanding_balance.sql` - Original migration
- `backend/src/main/resources/db/migration/V90__Fix_outstanding_balance_direct.sql` - Backup migration
- `backend/src/main/java/com/minet/sacco/service/LoanDisbursementService.java` - Code fix (already applied)

## Verification
After applying the fix, run this query to verify:
```sql
SELECT id, loan_number, status, total_repayable, outstanding_balance, 
       (total_repayable - outstanding_balance) as repaid_amount
FROM loans 
WHERE status IN ('DISBURSED', 'REPAID')
ORDER BY id;
```

Expected output:
```
id | loan_number | status | total_repayable | outstanding_balance | repaid_amount
13 | LN-2026-00003 | DISBURSED | 280000 | 280000 | 0
```

## Timeline
- **Option 1 (Direct SQL):** 30 seconds
- **Option 2 (Restart):** 2-3 minutes

**Recommendation:** Use Option 1 for immediate fix before your demo tomorrow.
