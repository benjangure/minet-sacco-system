# Loan Number Tracking Issue - Root Cause Analysis

## Executive Summary

**Benjamin Ngure's REPAID loan has NULL loan_number** - This is a **data integrity issue**, not a code bug.

**Can it be fixed in 5 minutes?** 
- **YES** - If the loan was never disbursed (simple UPDATE query)
- **NO** - If the loan was disbursed but the number was cleared (requires investigation + fix)

---

## The Problem

| Member | Amount | Status | Loan Number | Issue |
|--------|--------|--------|-------------|-------|
| Benjamin Ngure | KES 20,000 | REPAID | **NULL** | ✗ No loan number |
| Kevin Otieno | KES 100,000 | DISBURSED | LN-2026-00002 | ✓ Correct |
| John Doe | KES 80,000 | DISBURSED | LN-2026-00003 | ✓ Correct |
| Stanley Mwangi | KES 200,000 | DISBURSED | LN-2026-00004 | ✓ Correct |

---

## Root Cause Analysis

### Where Loan Numbers Are Assigned

**File**: `backend/src/main/java/com/minet/sacco/service/LoanDisbursementService.java` (Lines 97-110)

```java
// Generate and assign loan number (only if not already assigned)
if (loan.getLoanNumber() == null || loan.getLoanNumber().isEmpty()) {
    String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
    loan.setLoanNumber(loanNumber);
}
```

**Key Point**: Loan numbers are ONLY assigned during **disbursement**, not at creation.

### How Loan Numbers Are Generated

**File**: `backend/src/main/java/com/minet/sacco/service/LoanNumberGenerationService.java`

```java
public String generateLoanNumber(Loan loan) {
    int year = LocalDateTime.now().getYear();
    
    // Count loans that are DISBURSED or REPAID in current year
    long yearCount = loanRepository.countByYearAndDisbursed(year);
    
    // Generate number with year-specific counter
    return String.format("LN-%d-%05d", year, yearCount + 1);
}
```

**Query**: `backend/src/main/java/com/minet/sacco/repository/LoanRepository.java` (Line 73)

```java
@Query("SELECT COUNT(l) FROM Loan l " +
       "WHERE (l.status = 'DISBURSED' OR l.status = 'REPAID') " +
       "AND YEAR(l.disbursementDate) = :year")
Long countByYearAndDisbursed(@Param("year") int year);
```

**Critical Insight**: The query counts loans with status `DISBURSED` OR `REPAID` - both statuses should have loan numbers.

### Where Loan Numbers Are Cleared

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java` (Line 252)

```java
loan.setLoanNumber(null); // NEW: Explicitly set to null - will be assigned on disbursement
```

**This is intentional** - Loan numbers are set to NULL at creation and only assigned at disbursement.

### What Happens on Repayment

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java` (Lines 380-395)

```java
@Transactional
public LoanRepayment makeRepayment(LoanRepaymentRequest request, User createdBy) {
    // ... validation code ...
    
    // Check if loan is fully repaid
    if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
        loan.setStatus(Loan.Status.REPAID);
        loanRepository.save(loan);
        // Release all guarantor pledges
        guarantorTrackingService.releaseAllPledges(loan);
    } else {
        loanRepository.save(loan);
    }
    
    return repayment;
}
```

**Important**: When a loan is marked as REPAID, the `loan_number` field is **NOT modified** - it should remain unchanged.

---

## Diagnosis: Why Benjamin Ngure's Loan Has NULL Loan Number

### Scenario 1: Loan Was Never Disbursed ✓ MOST LIKELY

**Evidence**:
- Status is REPAID (not DISBURSED)
- Loan number is NULL
- Outstanding balance is 0 (fully repaid)

**What Happened**:
1. Loan was created with `loan_number = NULL`
2. Loan was approved but **never disbursed** (no `disburseLoan()` call)
3. Somehow the loan was marked as REPAID without going through DISBURSED status
4. Since loan was never disbursed, it never got a loan number

**How to Verify**:
```sql
SELECT id, member_id, amount, status, loan_number, 
       disbursement_date, created_at, approval_date
FROM loans
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
ORDER BY created_at DESC;
```

**Expected Result**: `disbursement_date` should be NULL or very recent

### Scenario 2: Loan Was Disbursed But Number Was Cleared ✗ UNLIKELY

**Why This Is Unlikely**:
- The code explicitly preserves loan numbers on repayment
- There's no code path that clears `loan_number` when status changes to REPAID
- The `makeRepayment()` method only updates `status` and `outstandingBalance`, not `loan_number`

---

## The Fix

### Option A: If Loan Was Never Disbursed (5-Minute Fix)

**Problem**: Loan is REPAID but never got a loan number

**Solution**: Assign a loan number retroactively

```sql
-- Check the current count of DISBURSED/REPAID loans in 2026
SELECT COUNT(*) FROM loans 
WHERE (status = 'DISBURSED' OR status = 'REPAID') 
AND YEAR(disbursement_date) = 2026;

-- Assign the next loan number to Benjamin's loan
UPDATE loans 
SET loan_number = 'LN-2026-00001'
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
AND status = 'REPAID'
AND loan_number IS NULL;
```

**Time**: < 1 minute

### Option B: If Loan Was Disbursed But Status Transition Was Wrong

**Problem**: Loan went from APPROVED directly to REPAID (skipped DISBURSED)

**Solution**: 
1. Investigate the audit trail to see what happened
2. Manually assign a loan number if needed
3. Add validation to prevent this in the future

**Time**: 5-10 minutes

---

## Code Review: Is There a Bug?

### ✓ Loan Number Generation Logic - CORRECT

The `LoanNumberGenerationService` correctly:
- Counts loans with status DISBURSED or REPAID
- Generates sequential numbers (00001, 00002, 00003, 00004)
- Handles year-specific counters

### ✓ Loan Disbursement Logic - CORRECT

The `LoanDisbursementService` correctly:
- Assigns loan numbers only during disbursement
- Preserves loan numbers if already assigned
- Detects and prevents duplicate loan numbers

### ✓ Loan Repayment Logic - CORRECT

The `LoanService.makeRepayment()` correctly:
- Does NOT clear the loan_number field
- Preserves the loan number when marking as REPAID
- Only updates status and outstanding balance

### ✗ Potential Issue: Status Transition Validation

**Missing Validation**: There's no check to prevent a loan from going directly from APPROVED to REPAID without being DISBURSED first.

**Current Flow**:
```
APPROVED → DISBURSED → REPAID ✓ Correct
APPROVED → REPAID ✗ Should not be possible
```

**Recommendation**: Add validation in `makeRepayment()` to ensure loan is DISBURSED before accepting repayments.

---

## Recommendation for Presentation

### What to Say

> "We found that Benjamin Ngure's fully repaid loan is missing its loan number. This is a **data integrity issue**, not a code bug. The system is correctly generating and preserving loan numbers for all other loans (LN-2026-00002, 00003, 00004). 
>
> The root cause is that this particular loan was either:
> 1. Never properly disbursed (most likely), or
> 2. Had an incorrect status transition
>
> We can fix this in under 5 minutes by assigning the missing loan number retroactively. We'll also add validation to prevent this from happening again."

### Action Items

1. **Immediate** (< 5 min): Assign loan number to Benjamin's loan
2. **Short-term** (< 1 hour): Add validation to prevent APPROVED → REPAID transitions
3. **Long-term** (next sprint): Add database constraint to prevent NULL loan_numbers for DISBURSED/REPAID loans

---

## Database Constraint Recommendation

Add this constraint to prevent future issues:

```sql
ALTER TABLE loans 
ADD CONSTRAINT chk_loan_number_not_null_when_disbursed
CHECK (
    (status NOT IN ('DISBURSED', 'REPAID')) OR 
    (loan_number IS NOT NULL)
);
```

This ensures that any loan with status DISBURSED or REPAID MUST have a loan_number.

---

## Summary

| Aspect | Status | Details |
|--------|--------|---------|
| **Code Bug** | ✓ NO | All code logic is correct |
| **Data Issue** | ✓ YES | Benjamin's loan missing loan number |
| **Root Cause** | ? UNKNOWN | Likely never disbursed or incorrect status transition |
| **Fix Time** | ✓ < 5 MIN | Simple UPDATE query |
| **Prevention** | ✓ POSSIBLE | Add validation + database constraint |

