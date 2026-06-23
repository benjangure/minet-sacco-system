# PHASE 5 — GL & Reporting Verification Report

**Status:** ⚠️ SILENT BREAKAGE IDENTIFIED — Requires fixes before deployment

**Date:** 6/19/2026

---

## Executive Summary

Phase 4 stopped importing `totalInterest` and `totalRepayable` for migrated loans. However, **multiple backend services and frontend components still reference these fields**, creating silent breakage:

- **GL posting** calculates interest income from `totalInterest` (GLCalculationService)
- **Reporting** displays repayment progress using `totalRepayable` (ReportsService, ReportExportService)
- **Eligibility** calculations depend on `totalRepayable` (EligibilityCalculationService)
- **Dashboard & member portals** show repayment progress bars based on these fields
- **Guarantor tracking** uses `totalRepayable` for pledge calculations

**Critical Issue:** For migrated loans where Phase 4 doesn't set `totalInterest` or `totalRepayable`, these fields will be **NULL**, causing:
- Progress bars to break (division by zero in frontend)
- GL posting to undercount interest income
- Reports to misrepresent repayment status

---

## Files with Silent Breakage Risk

### Backend Services

#### 1. **GLCalculationService.java** (Line 295)
```java
.map(loan -> loan.getTotalInterest() != null ? loan.getTotalInterest() : ZERO)
```
**Risk:** Calculates interest income for GL posting. If migrated loans have NULL `totalInterest`, interest income is underreported.

**Impact:** GL interest income accounts may not match expected amounts; financial statements inaccurate.

---

#### 2. **GuarantorTrackingService.java** (Line 54)
```java
BigDecimal totalRepayable = loan.getTotalRepayable();
```
**Risk:** Uses `totalRepayable` without null check to calculate pledge reductions. If NULL, will crash with NullPointerException.

**Impact:** Migrated loans with guarantors will fail during repayment processing.

---

#### 3. **BulkProcessingService.java** (Line 678)
```java
BigDecimal totalRepayable = updatedLoan.getTotalRepayable() != null ? updatedLoan.getTotalRepayable() : updatedLoan.getOutstandingBalance();
```
**Status:** ✅ Safe — Has defensive null check. Falls back to outstanding balance.

---

#### 4. **LoanRepaymentService.java** (Line 207, 254)
```java
BigDecimal monthlyPayment = loan.getMonthlyRepayment();
// ...
loan.getTotalRepayable()
```
**Risk:** Used in amortization schedule calculation. If NULL, affects remaining months computation.

**Impact:** Repayment schedule display will show incorrect values.

---

#### 5. **ReportsService.java** (Line 335)
```java
entry.setMonthlyRepayment(loan.getMonthlyRepayment());
```
**Risk:** Populates monthly repayment in report DTO. NULL values flow to reports.

**Impact:** Reports display empty or zero monthly repayment for migrated loans.

---

#### 6. **ReportExportService.java** (Line 442, 500)
```java
row.createCell(6).setCellValue(entry.getMonthlyRepayment() != null ? entry.getMonthlyRepayment().doubleValue() : 0.0);
```
**Status:** ✅ Safe — Has null check, defaults to 0.0.

---

#### 7. **EligibilityCalculationService.java** (Line 112, 247, 330, 398)
```java
if (loan.getTotalInterest() != null) {
    totalSelfGuaranteedInterest = totalSelfGuaranteedInterest.add(loan.getTotalInterest());
}
// ...
BigDecimal totalRepayable = loan.getTotalRepayable();
```
**Risk:** Line 112 has null check, but lines 247, 330, 398 use `getTotalRepayable()` without null checks.

**Impact:** Eligibility checks may fail or produce incorrect results for future loan applications if borrower has migrated loans.

---

#### 8. **LoanDisbursementService.java** (Line 83-92)
```java
if (loan.getTotalInterest() == null) {
    loan.setTotalInterest(BigDecimal.ZERO);
}
if (loan.getTotalRepayable() == null) {
    loan.setTotalRepayable(principal);
}
if (loan.getMonthlyRepayment() == null) {
    loan.setMonthlyRepayment(BigDecimal.ZERO);
}
```
**Status:** ✅ Safe — Sets defaults for new loans during disbursement.

---

### Frontend Components (Display Risk)

#### 1. **Loans.tsx** (Lines 1150, 1154, 1158, 1173-1183)
- Displays `totalInterest`, `totalRepayable`, `monthlyRepayment`
- Uses these for progress bar calculation: `(totalRepayable - outstandingBalance) / totalRepayable * 100`
- **Risk:** If NULL, division by zero → NaN → progress bar broken

#### 2. **MemberDashboard.tsx** (Lines 1034, 1148, 1162-1180)
- Shows `monthlyRepayment` in loan summary
- Uses `totalRepayable` for repayment progress calculation
- **Risk:** Division by zero if NULL

#### 3. **Index.tsx** (Lines 123-129)
- Dashboard dashboard calculates "outstanding interest"
- Uses `totalRepayable` and `totalInterest` without null checks
- **Risk:** NaN in dashboard display

#### 4. **LoanMigration.tsx** (Line 473)
- Displays `monthlyRepayment` in migration results table
- **Risk:** Shows NULL/empty for migrated loans

---

## Root Cause Analysis

**Phase 4 Decision:** Do NOT import or set `totalInterest`/`totalRepayable` for migrated loans.

**Problem:** This decision was made to stop calculating interest upfront, but:
1. These fields already existed in the database schema
2. Multiple services assume they are either set OR null-checked
3. Not all code paths have defensive null checks
4. Frontend displays break on division by zero

**The Gap:** Phase 4 updated `LoanMigrationService` but didn't account for:
- Existing code that reads these fields
- Frontend components that depend on them
- GL posting logic

---

## Required Fixes

### Fix 1: GuarantorTrackingService.java (CRITICAL)
**File:** `backend/src/main/java/com/minet/sacco/service/GuarantorTrackingService.java`

Add null check at line 54:
```java
// Line 54: Add null check
BigDecimal totalRepayable = loan.getTotalRepayable();
if (totalRepayable == null) {
    totalRepayable = loan.getOutstandingBalance() != null ? 
        loan.getOutstandingBalance() : loan.getAmount();
}
```

---

### Fix 2: EligibilityCalculationService.java (CRITICAL)
**File:** `backend/src/main/java/com/minet/sacco/service/EligibilityCalculationService.java`

Add null checks at lines 247, 330, 398:
```java
// Line 247, 330, 398: Replace direct use of getTotalRepayable() with:
BigDecimal totalRepayable = loan.getTotalRepayable();
if (totalRepayable == null) {
    // For migrated loans without totalRepayable, use outstanding balance as proxy
    totalRepayable = loan.getOutstandingBalance() != null ? 
        loan.getOutstandingBalance() : loan.getAmount();
}
```

---

### Fix 3: GLCalculationService.java (HIGH)
**File:** `backend/src/main/java/com/minet/sacco/service/GLCalculationService.java`

Line 295 already has null check (using ternary), but clarify intent:
```java
// Migrated loans without totalInterest default to zero (no upfront interest calculated)
.map(loan -> loan.getTotalInterest() != null ? loan.getTotalInterest() : ZERO)
```

**This is correct.** Document that migrated loans show zero accrued interest upfront; actual interest is tracked via Transaction records during repayment.

---

### Fix 4: LoanRepaymentService.java (HIGH)
**File:** `backend/src/main/java/com/minet/sacco/service/LoanRepaymentService.java`

Line 254: Add null check:
```java
BigDecimal totalRepayable = loan.getTotalRepayable() != null ? 
    loan.getTotalRepayable() : loan.getOutstandingBalance();
```

---

### Fix 5: Frontend - Loans.tsx (MEDIUM)
**File:** `minetsacco-main/src/pages/Loans.tsx`

Add fallback logic for repayment progress calculation (lines 1173-1183):
```tsx
{selectedLoanForDetails.totalRepayable && selectedLoanForDetails.totalRepayable > 0
  ? `${Math.round(((selectedLoanForDetails.totalRepayable - selectedLoanForDetails.outstandingBalance) / selectedLoanForDetails.totalRepayable) * 100)}%`
  : selectedLoanForDetails.outstandingBalance === 0 ? "100%" : "0%"}
```

---

### Fix 6: Frontend - MemberDashboard.tsx (MEDIUM)
**File:** `minetsacco-main/src/pages/MemberDashboard.tsx`

Add fallback at line 1180:
```tsx
style={{ width: loan.totalRepayable && loan.totalRepayable > 0 
  ? `${Math.min(Math.max(0, ((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100), 100)}%`
  : loan.outstandingBalance === 0 ? "100%" : "0%" }}
```

---

### Fix 7: Frontend - Index.tsx (MEDIUM)
**File:** `minetsacco-main/src/pages/Index.tsx`

Add validation at lines 126-129:
```tsx
if (outstanding <= 0 || !totalRepayable || totalRepayable <= 0 || !totalInterest || totalInterest <= 0) return sum;
```

---

## Testing Checklist

After implementing fixes:

- [ ] Migrate a loan and verify `totalInterest` and `totalRepayable` are NULL in database
- [ ] Record a repayment on migrated loan → guarantor pledge tracking should not crash
- [ ] Verify GL posting calculates interest income correctly (should be 0 for migrated loans at migration)
- [ ] Run eligibility check on member with migrated loans → should not crash
- [ ] View loan details page → progress bar should show 0% or calculate correctly
- [ ] View member dashboard → no NaN values displayed
- [ ] Check reports for migrated loans → should display correctly
- [ ] Export loan report to Excel → no errors

---

## Summary of Changes Required

| Component | Issue | Fix | Priority |
|-----------|-------|-----|----------|
| GuarantorTrackingService | NPE on NULL totalRepayable | Add null check, fallback to outstanding balance | CRITICAL |
| EligibilityCalculationService | Multiple NPE paths (lines 247, 330, 398) | Add null checks, fallback logic | CRITICAL |
| LoanRepaymentService | NULL totalRepayable in schedule | Add null check | HIGH |
| GLCalculationService | Already safe (has null check) | Verify & document | NONE |
| Loans.tsx | Division by zero in progress bar | Add fallback calculation | MEDIUM |
| MemberDashboard.tsx | Division by zero in progress bar | Add fallback calculation | MEDIUM |
| Index.tsx | NaN in dashboard display | Add null validation | MEDIUM |

---

## Deployment Impact

**Without fixes:** Migrated loans will cause crashes when:
- Recording repayments (guarantor tracking fails)
- Checking member eligibility
- Viewing loan details (UI breaks)

**With fixes:** Migrated loans behave safely with fallback logic; no silent failures.

---

## Recommendation

Implement all fixes before deploying Phase 4 changes to production. This prevents silent data corruption and ensures migrated loans operate correctly alongside new loans.
