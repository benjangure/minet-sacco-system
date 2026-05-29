# Loan Number Tracking Issue - Analysis for Presentation

## Current Status

Looking at the Loans table, there is an **inconsistency in loan number assignment**:

### Observations

| Member | Amount | Status | Loan Number | Issue |
|--------|--------|--------|-------------|-------|
| Benjamin Ngure | KES 20,000 | REPAID | **NULL** | ✗ Fully repaid but NO loan number |
| Kevin Otieno | KES 100,000 | DISBURSED | LN-2026-00002 | ✓ Correct |
| John Doe | KES 80,000 | DISBURSED | LN-2026-00003 | ✓ Correct |
| Collins Barasa | KES 30,000 | PENDING_LOAN_OFFICER_REVIEW | **NULL** | ✓ Correct (not yet disbursed) |
| Stanley Mwangi | KES 200,000 | DISBURSED | LN-2026-00004 | ✓ Correct |

## The Problem

**Benjamin Ngure's loan (REPAID status) has NO loan number** - it shows as NULL.

This violates the requirement that:
> "Once a loan is fully repaid, it should keep its original loan number for proper audit trail and tracking purposes."

## Root Cause

The loan number was likely **never assigned** during disbursement, or it was **cleared/nullified** when the loan was marked as REPAID.

### Possible Scenarios

1. **Loan was never disbursed** - If a loan is repaid without being disbursed, it wouldn't have a loan number
2. **Loan number was cleared on repayment** - Some process might be clearing the loan_number field when status changes to REPAID
3. **Data migration issue** - The loan might have been migrated without proper loan number assignment

## Expected Behavior

**All loans should maintain their loan number throughout their lifecycle:**

```
PENDING → PENDING_LOAN_OFFICER_REVIEW → APPROVED → DISBURSED → REPAID
                                                        ↓
                                              Loan number assigned here
                                              (should persist to REPAID)
```

## What Should Happen

Benjamin Ngure's REPAID loan should show:
- **Loan Number**: LN-2026-00001 (or similar)
- **Status**: REPAID
- **Outstanding Balance**: 0
- **Repayment %**: 100%

## Impact

- ✗ Cannot track fully repaid loans by loan number
- ✗ Audit trail is incomplete
- ✗ Compliance reporting may be affected
- ✗ Member records lack proper documentation

## Recommendation for Presentation

**This is a data integrity issue that needs investigation:**

1. **Check the database** - Verify if the loan_number was ever assigned to Benjamin Ngure's loan
2. **Review the repayment process** - Ensure loan numbers are NOT cleared when marking loans as REPAID
3. **Implement validation** - Add database constraint to prevent loan_number from being NULL for DISBURSED or REPAID loans
4. **Audit trail** - Check if there's a process that's clearing loan numbers on repayment

## Status

**Not yet fixed** - This is a data integrity issue that requires investigation before the presentation.

The system is correctly generating new loan numbers (LN-2026-00002, 00003, 00004), but there's an issue with how loan numbers are being managed for fully repaid loans.
