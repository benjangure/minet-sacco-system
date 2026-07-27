# Loan Number Tracking Issue - Technical Deep Dive

## Overview

Benjamin Ngure's REPAID loan has `loan_number = NULL`. This document provides a complete technical analysis of where this issue originates and how to fix it.

---

## Part 1: How Loan Numbers Are Supposed to Work

### Loan Lifecycle

```
1. CREATE LOAN
   ├─ loan_number = NULL (intentional)
   ├─ status = PENDING
   └─ disbursement_date = NULL

2. APPROVE LOAN
   ├─ loan_number = NULL (still not assigned)
   ├─ status = PENDING_LOAN_OFFICER_REVIEW → ... → APPROVED
   └─ disbursement_date = NULL

3. DISBURSE LOAN ← LOAN NUMBER ASSIGNED HERE
   ├─ loan_number = "LN-2026-00001" (generated)
   ├─ status = DISBURSED
   └─ disbursement_date = NOW()

4. REPAY LOAN
   ├─ loan_number = "LN-2026-00001" (PRESERVED)
   ├─ status = REPAID
   └─ disbursement_date = (unchanged)
```

### Key Principle

**Loan numbers are immutable once assigned and should persist through the entire loan lifecycle.**

---

## Part 2: Code Analysis

### 1. Loan Creation (LoanService.applyForLoan)

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java` (Line 252)

```java
loan.setLoanNumber(null); // NEW: Explicitly set to null - will be assigned on disbursement
```

**Purpose**: Loan numbers are not assigned at creation. They're assigned only at disbursement.

**Status**: ✓ CORRECT

---

### 2. Loan Disbursement (LoanDisbursementService.disburseLoan)

**File**: `backend/src/main/java/com/minet/sacco/service/LoanDisbursementService.java` (Lines 97-110)

```java
// Generate and assign loan number (only if not already assigned)
if (loan.getLoanNumber() == null || loan.getLoanNumber().isEmpty()) {
    String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
    loan.setLoanNumber(loanNumber);
} else {
    // Loan already has a number, but verify it's not a duplicate from a failed attempt
    // Check if another loan already has this number
    boolean isDuplicate = loanRepository.existsByLoanNumberAndIdNot(loan.getLoanNumber(), loan.getId());
    if (isDuplicate) {
        // Another loan has this number, regenerate
        String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
        loan.setLoanNumber(loanNumber);
    }
}
```

**Purpose**: 
- Assign loan number if not already assigned
- Detect and prevent duplicate loan numbers
- Preserve loan number if already assigned

**Status**: ✓ CORRECT

---

### 3. Loan Number Generation (LoanNumberGenerationService.generateLoanNumber)

**File**: `backend/src/main/java/com/minet/sacco/service/LoanNumberGenerationService.java`

```java
public String generateLoanNumber(Loan loan) {
    int year = LocalDateTime.now().getYear();
    
    // Count loans that are DISBURSED or REPAID in current year
    // Both statuses have been assigned loan numbers
    long yearCount = loanRepository.countByYearAndDisbursed(year);
    
    // Generate number with year-specific counter
    return String.format("LN-%d-%05d", year, yearCount + 1);
}
```

**Logic**:
1. Get current year (2026)
2. Count loans with status DISBURSED or REPAID in 2026
3. Add 1 to get next sequence number
4. Format as "LN-2026-00001"

**Example**:
- If 3 loans are DISBURSED/REPAID in 2026
- Next loan gets: LN-2026-00004

**Status**: ✓ CORRECT

---

### 4. Loan Number Counting Query (LoanRepository.countByYearAndDisbursed)

**File**: `backend/src/main/java/com/minet/sacco/repository/LoanRepository.java` (Line 73)

```java
@Query("SELECT COUNT(l) FROM Loan l " +
       "WHERE (l.status = 'DISBURSED' OR l.status = 'REPAID') " +
       "AND YEAR(l.disbursementDate) = :year")
Long countByYearAndDisbursed(@Param("year") int year);
```

**Purpose**: Count all loans that have been assigned loan numbers (DISBURSED or REPAID)

**Status**: ✓ CORRECT

---

### 5. Loan Repayment (LoanService.makeRepayment)

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java` (Lines 380-395)

```java
@Transactional
public LoanRepayment makeRepayment(LoanRepaymentRequest request, User createdBy) {
    Loan loan = loanRepository.findById(request.getLoanId())
            .orElseThrow(() -> new RuntimeException("Loan not found"));

    if (loan.getStatus() != Loan.Status.DISBURSED) {
        throw new RuntimeException("Loan is not in disbursed status");
    }

    // ... create repayment record ...

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

**Key Points**:
- ✓ Only accepts repayments for DISBURSED loans
- ✓ Does NOT modify loan_number field
- ✓ Only updates status and outstanding_balance
- ✓ Loan number is preserved when marking as REPAID

**Status**: ✓ CORRECT

---

## Part 3: Root Cause Analysis

### Why Benjamin's Loan Has NULL Loan Number

**Scenario**: Loan is REPAID but has NULL loan_number

**Possible Causes**:

#### Cause 1: Loan Was Never Disbursed (MOST LIKELY)

**Evidence**:
- Status is REPAID (not DISBURSED)
- Loan number is NULL
- Outstanding balance is 0

**What Happened**:
1. Loan was created with `loan_number = NULL`
2. Loan was approved
3. **Loan was never disbursed** (no `disburseLoan()` call)
4. Somehow loan was marked as REPAID without going through DISBURSED
5. Since loan was never disbursed, it never got a loan number

**How to Verify**:
```sql
SELECT id, member_id, amount, status, loan_number, 
       disbursement_date, created_at, approval_date
FROM loans
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
ORDER BY created_at DESC;
```

**Expected Result**: `disbursement_date` should be NULL or very recent

#### Cause 2: Loan Was Disbursed But Number Was Cleared (UNLIKELY)

**Why This Is Unlikely**:
- The code explicitly preserves loan numbers on repayment
- There's no code path that clears `loan_number` when status changes to REPAID
- The `makeRepayment()` method only updates `status` and `outstandingBalance`

**Code Evidence**:
```java
// In makeRepayment() - only these fields are modified:
loan.setStatus(Loan.Status.REPAID);
loan.setOutstandingBalance(newOutstanding);
// loan_number is NOT touched
```

#### Cause 3: Direct Database Manipulation (UNLIKELY)

**Why This Is Unlikely**:
- No evidence of direct SQL updates
- System is working correctly for other loans
- This would be a manual error, not a code bug

---

## Part 4: The Fix

### Quick Fix (< 5 minutes)

**Step 1**: Verify the issue
```sql
SELECT id, member_id, amount, status, loan_number, disbursement_date
FROM loans
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
AND status = 'REPAID'
AND loan_number IS NULL;
```

**Step 2**: Assign the missing loan number
```sql
UPDATE loans 
SET loan_number = 'LN-2026-00001'
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
AND status = 'REPAID'
AND loan_number IS NULL;
```

**Step 3**: Verify the fix
```sql
SELECT id, member_id, amount, status, loan_number, disbursement_date
FROM loans
WHERE member_id = (SELECT id FROM members WHERE first_name = 'Benjamin' AND last_name = 'Ngure')
AND status = 'REPAID';
```

---

### Permanent Fix (Add Validation)

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java`

**Add this validation to `makeRepayment()` method**:

```java
@Transactional
public LoanRepayment makeRepayment(LoanRepaymentRequest request, User createdBy) {
    Loan loan = loanRepository.findById(request.getLoanId())
            .orElseThrow(() -> new RuntimeException("Loan not found"));

    // VALIDATION: Loan must be DISBURSED before accepting repayments
    if (loan.getStatus() != Loan.Status.DISBURSED) {
        throw new RuntimeException("Loan must be DISBURSED before accepting repayments. Current status: " + loan.getStatus());
    }
    
    // VALIDATION: Loan must have a loan number (should have been assigned at disbursement)
    if (loan.getLoanNumber() == null || loan.getLoanNumber().isEmpty()) {
        throw new RuntimeException("Loan must have a loan number to accept repayments. This indicates a disbursement issue.");
    }

    // ... rest of the method ...
}
```

---

### Database Constraint (Prevent Future Issues)

**Add this constraint to the loans table**:

```sql
ALTER TABLE loans 
ADD CONSTRAINT chk_loan_number_not_null_when_disbursed
CHECK (
    (status NOT IN ('DISBURSED', 'REPAID')) OR 
    (loan_number IS NOT NULL)
);
```

**Effect**: Any attempt to set a loan to DISBURSED or REPAID status without a loan_number will fail.

---

## Part 5: Summary

### Code Quality Assessment

| Component | Status | Notes |
|-----------|--------|-------|
| Loan number generation | ✓ CORRECT | Properly increments and formats |
| Loan number assignment | ✓ CORRECT | Assigned at disbursement, preserved on repayment |
| Loan number preservation | ✓ CORRECT | Not cleared when marking as REPAID |
| Duplicate detection | ✓ CORRECT | Prevents duplicate loan numbers |
| Validation | ✗ MISSING | No check to prevent APPROVED → REPAID without DISBURSED |

### Root Cause

**Most Likely**: Benjamin's loan was never properly disbursed, so it never received a loan number.

### Fix Time

- **Quick Fix**: < 1 minute (UPDATE query)
- **Validation Fix**: < 5 minutes (add code check)
- **Constraint Fix**: < 1 minute (ALTER TABLE)

### Recommendation

1. **Immediate**: Run the UPDATE query to assign the missing loan number
2. **Short-term**: Add validation to prevent APPROVED → REPAID transitions
3. **Long-term**: Add database constraint to enforce loan_number NOT NULL for DISBURSED/REPAID loans

---

## Appendix: Related Code Files

### Files Involved

1. **LoanService.java** - Loan creation, approval, repayment
2. **LoanDisbursementService.java** - Loan disbursement and loan number assignment
3. **LoanNumberGenerationService.java** - Loan number generation logic
4. **LoanRepository.java** - Database queries for loan counting

### Key Methods

- `LoanService.applyForLoan()` - Creates loan with `loan_number = NULL`
- `LoanService.approveLoan()` - Approves loan, still `loan_number = NULL`
- `LoanDisbursementService.disburseLoan()` - Assigns loan number here
- `LoanService.makeRepayment()` - Marks loan as REPAID, preserves loan_number
- `LoanNumberGenerationService.generateLoanNumber()` - Generates unique loan number
- `LoanRepository.countByYearAndDisbursed()` - Counts loans for sequence number

