# Interest Collected Column - Excel Template Update

## Summary
Added the "Interest Collected" column to the loan migration Excel template at **Column 9**, enabling treasurers to capture historical interest during loan migration.

## Changes Made

### 1. Backend - LoanMigrationService.java
**File:** `backend/src/main/java/com/minet/sacco/service/LoanMigrationService.java`

Updated `generateLoanMigrationTemplate()` method:
- Added header at Column 9: `"Interest Collected KES (optional - historical interest from migrated loans)"`
- Updated example row 1: Set interest collected to 15,000 (example for migrated loan)
- Updated example row 2: Set interest collected to 0 (for new loans)
- Updated example row 4: Set interest collected to 25,000 (example for update)
- Adjusted guarantor column indices from 10-22 to 11-23 to accommodate the new column

### 2. Excel Parser - ExcelParserService.java
**File:** `backend/src/main/java/com/minet/sacco/service/ExcelParserService.java`

The parser was already correctly configured:
- Column 9 reads: `item.setInterestCollected(getCellValueAsBigDecimal(row.getCell(9)))`
- Guarantors start at Column 11 (correctly shifted)

### 3. Frontend - Loans.tsx
**File:** `minetsacco-main/src/pages/Loans.tsx`

Already integrated:
- Phase A edit form includes "Interest Collected (KES)" field
- Only visible for migrated loans (`migrationStatus === "MIGRATED"`)
- Help text explains: "Interest already collected during loan repayment period. Updates interest remaining calculation."
- Validation: Must be >= 0 and <= total interest

## Column Mapping - Excel Template

| Col | Field | Type | Notes |
|-----|-------|------|-------|
| 0 | Loan Number | String | Blank = CREATE, Populated = UPDATE |
| 1 | Employee ID | String | Required for CREATE |
| 2 | Loan Product Name | String | Required for CREATE |
| 3 | Principal Amount | Decimal | Required for CREATE |
| 4 | Term Months | Integer | Optional |
| 5 | Interest Rate % | Decimal | Optional (uses product default) |
| 6 | Disbursement Date | Date (DD/MM/YYYY) | Optional |
| 7 | Loan Status | String | DISBURSED, REPAID, DEFAULTED |
| 8 | Outstanding Balance | Decimal | Optional |
| **9** | **Interest Collected KES** | **Decimal** | **NEW - Historical interest from migration** |
| 10 | Guarantorship Type | String | NORMAL, SELF |
| 11-22 | Guarantor Details | Mixed | 6 pairs of (Employee ID + Pledge Amount) |
| 23 | Purpose | String | Optional |

## How to Use

### For New Loans (CREATE mode)
- Leave Column 9 blank or set to 0
- Set Loan Number to blank
- Fill Employee ID, Loan Product, Principal, Term, etc.

### For Migrated Loans (UPDATE or CREATE)
- Column 9 = Total interest already collected from the old system
- For example: If a migrated loan had collected KES 15,000 in interest, enter 15000 in Column 9
- System will calculate: `Interest Remaining = Total Interest - Interest Collected`

### For Existing Loans (UPDATE mode)
- Populate Loan Number with the loan's number
- Set Column 9 to the cumulative interest collected to date
- System will update the interest_collected field and recalculate interest remaining

## Backend Validation

In `LoanMigrationService.validateLoanMigrationItem()`:
```
- Interest Collected must be >= 0
- Interest Collected must be <= calculated total interest
- If invalid: Error message indicates the constraint violation
```

## Database

**Table:** `loans`
- Column: `interest_collected` (DECIMAL(15,2), nullable)

**Table:** `loan_migration_items`
- Column: `interest_collected` (DECIMAL(15,2), nullable)
- Used to store historical interest during import

## Testing Checklist

- [ ] Download template from loan migration page
- [ ] Verify Column 9 header is "Interest Collected KES (optional...)"
- [ ] Create a test loan with interest collected = 5000
- [ ] Verify dashboard "Interest Collected" metric includes the 5000
- [ ] Edit an existing migrated loan in Phase A and update interest collected
- [ ] Verify audit trail logs the change with old/new values
- [ ] Verify interest remaining = total interest - interest collected

## Important Notes

1. **Read-only after first migration:** Once a loan is migrated with an interest_collected value, treasurers can only update it via Phase A editing, not by re-importing
2. **Locked principal:** Principal cannot be edited after migration to protect historical calculation integrity
3. **Audit trail:** All interest collected updates are logged with timestamp, user, and old/new values
4. **Guarantor impact:** Interest collected does NOT affect guarantor calculations—only principal matters

---
**Status:** ✅ Complete
**Date:** June 29, 2026
**Backend Version:** Running
**Frontend:** Ready for testing
