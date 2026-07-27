# Loan Migration Template Analysis

## Overview
The loan migration feature allows importing historical loan records (DISBURSED, REPAID, DEFAULTED) from a previous system. It's a **one-time bulk import process** that bypasses the normal loan approval workflow.

---

## What Happens During Loan Migration

### 1. **File Upload & Processing**
- User downloads the CSV template from the UI
- Prepares an Excel file (.xlsx or .xls) with historical loan data
- Uploads the file through the **Loan Migration** page (not bulk processing)
- System validates each row against the template schema
- Successfully imported loans are created immediately (no approval step)
- Failed rows are logged with specific error messages

### 2. **Per-Loan Processing**
For each successfully imported loan:
- ✅ Loan record is created with all historical data
- ✅ Guarantors are created and frozen (if loan is DISBURSED)
- ✅ Savings are frozen for self-guaranteed active loans
- ✅ A LOAN_DISBURSEMENT transaction is created for audit trail
- ✅ Migration flag is set on the loan entity

### 3. **Guarantor Handling**
- **NORMAL type**: Requires 1-6 external guarantors; pledges must sum exactly to principal
- **SELF type**: Borrower guarantees their own loan; no external guarantors allowed
- Only **DISBURSED** loans have frozen guarantor savings; REPAID and DEFAULTED loans have guarantors released

---

## Template Structure & Mandatory Fields

### Column Layout (22 columns total)

| Col | Field Name | Type | Mandatory | Notes |
|-----|----------|------|-----------|-------|
| 0 | **Employee ID** | String | ✅ YES | Member must be pre-registered in system |
| 1 | **Loan Number** | String | ❌ NO | Optional; if blank, system generates LN-YYYY-XXXXX |
| 2 | **Loan Product Name** | String | ✅ YES | Must match an **active** loan product exactly (e.g., "Standard Loan") |
| 3 | **Principal Amount** | Decimal | ✅ YES | > 0; supports currency prefixes (Ksh 100,000.00) |
| 4 | **Term (Months)** | Integer | ✅ YES | > 0; loan duration in months |
| 5 | **Interest Rate %** | Decimal | ❌ NO | **Informational only** — system uses product's configured rate |
| 6 | **Disbursement Date** | Date | ✅ YES | Format: DD/MM/YYYY; cannot be in future |
| 7 | **Loan Status** | Enum | ✅ YES | Must be: **DISBURSED**, **REPAID**, or **DEFAULTED** |
| 8 | **Outstanding Balance** | Decimal | ✅ YES | ≥ 0; must be 0 for REPAID loans; ≤ principal |
| 9 | **Guarantorship Type** | Enum | ✅ YES | Must be: **NORMAL** or **SELF** |
| 10–21 | **Guarantor 1–6** (pairs) | String + Decimal | ⚠️ CONDITIONAL | See guarantor rules below |
| 22 | **Purpose** | String | ❌ NO | Optional loan purpose/description |

### Guarantor Columns (10–21)
Each guarantor requires **two columns**:
- **Col 10, 12, 14, 16, 18, 20**: Guarantor Employee ID (string)
- **Col 11, 13, 15, 17, 19, 21**: Guarantor Pledge Amount (decimal)

**Rules**:
- **NORMAL guarantorship**: 
  - Requires at least 1 guarantor
  - Pledges must sum exactly to principal (no more, no less)
  - Guarantor cannot be the borrower
  - Guarantor must be a registered member
  - Supports up to 6 guarantors
- **SELF guarantorship**: 
  - No external guarantors allowed
  - All guarantor columns must be empty/blank

---

## Alignment with Reducing Balance System

### ✅ **What IS Aligned**
1. **Monthly Repayment Calculation**: Uses reducing balance formula
   - Formula: `Principal × (Rate/12) × Term / (1 - (1 + Rate/12)^-Term)`
   - Stored on the loan for reference
   - Used to show expected monthly repayment on imported loans

2. **Outstanding Balance Snapshot**: Treated as a historical snapshot
   - System imports whatever balance is provided
   - No recalculation from principal/interest
   - Reflects the true current state at time of migration

3. **Phase 4 Loan Repayment Rules**: Future repayments on migrated loans follow Phase 2/4 logic
   - Any repayment on a migrated loan must include mandatory **principal/interest split**
   - Repayments cannot default to monthly amount automatically
   - User must specify how much is principal vs interest

### ⚠️ **What IS NOT Aligned (Design Decision)**
1. **Historical Interest NOT Imported**
   - System does NOT backfill `totalInterest` or `totalRepayable` from the old system
   - These fields remain null/unset for migrated loans
   - **Reason**: Migrated loans have unknown interest calculation methods (flat rate, reducing balance, simple interest, etc.)
   - **Impact**: The only reliable data point is `outstandingBalance`—a snapshot of what was owed

2. **Interest Calculation NOT Recalculated**
   - System treats outstanding balance as the single source of truth
   - Does not recalculate what the interest *should* be
   - This prevents introducing errors if old system used different interest method

3. **Imported Interest Rate Column is Informational Only**
   - The "Interest Rate %" column in the template is ignored
   - System uses the **product's configured rate** for all calculations
   - This ensures consistency with new loans going forward

---

## Field Validation Summary

### Required Validations
✅ Employee ID exists in system  
✅ Loan Product Name matches an active product (exactly)  
✅ Principal > 0  
✅ Term > 0  
✅ Disbursement Date is not in future  
✅ Loan Status is DISBURSED, REPAID, or DEFAULTED  
✅ Outstanding Balance ≥ 0  
✅ Outstanding Balance = 0 if status is REPAID  
✅ Outstanding Balance ≤ Principal  
✅ Guarantorship Type is NORMAL or SELF  

### Conditional Validations
🔹 **If NORMAL guarantorship**:
- At least 1 guarantor required
- All guarantors must exist in system
- All guarantor pledges > 0
- Pledges sum exactly to principal
- Guarantor ≠ Borrower

🔹 **If SELF guarantorship**:
- No guarantor columns should have data

---

## Example Template Rows

### ✅ NORMAL Guarantorship (DISBURSED)
```
EMP001 | [blank] | Standard Loan | 100000 | 12 | 15 | 15/01/2024 | DISBURSED | 75000 | NORMAL | EMP002 | 50000 | EMP003 | 50000 | ... [rest blank]
```
- 2 guarantors pledging 50k each = exactly 100k principal ✅

### ✅ SELF Guarantorship (REPAID)
```
EMP004 | [blank] | Emergency Loan | 50000 | 6 | 18 | 01/03/2024 | REPAID | 0 | SELF | [all blank] | ... [rest blank]
```
- Self-guarantee with 0 outstanding balance ✅
- No external guarantors ✅

### ❌ NORMAL Guarantorship (FAILED)
```
EMP005 | [blank] | Standard Loan | 100000 | 12 | 15 | 15/01/2024 | DISBURSED | 80000 | NORMAL | EMP006 | 60000 | EMP007 | 30000 | ... [rest blank]
```
- **Error**: Pledges total 90k but principal is 100k (must be exact match) ❌

---

## Important Notes for Users

### Before Uploading
- ✅ All borrowers must be pre-registered in the system
- ✅ All guarantors must be pre-registered in the system
- ✅ Loan product names must match exactly (case-sensitive advisable)
- ✅ For NORMAL guarantorship, pledges must sum to principal (not more, not less)

### During Upload
- Each row is validated independently
- If validation fails, row shows a specific error message
- Successfully validated rows are imported immediately
- Failed rows can be corrected and re-uploaded without affecting already-imported loans

### After Upload
- Migrated loans appear in member dashboards
- Active (DISBURSED) loans have frozen guarantor savings
- Future repayments must include principal/interest split (Phase 4 rules)
- Monthly repayment amounts shown are for reference only

---

## Impact on Reports

### Reports That INCLUDE Migrated Loans ✅

| Report | Impact | Details |
|--------|--------|---------|
| **Cashbook** | ✅ YES | LOAN_DISBURSEMENT transactions are created for migrated loans; appears in cash flow totals with `[MIGRATED]` label |
| **Trial Balance** | ✅ YES | Migrated loans affect member account balances equally; frozen savings shown for active loans |
| **Loan Register** | ✅ YES | ALL loans (migrated + new) included; shows principal, outstanding balance, monthly repayment |
| **Member Statement** | ✅ YES | LOAN_DISBURSEMENT transactions appear in member's transaction history |

### Reports That DO NOT Include Migrated Loans ❌

| Report | Status | Reason |
|--------|--------|--------|
| **General Ledger (GL) Reports** | ❌ NO GL entries created | Migration process does NOT create GL transactions; intentional design |
| **Income Statement** | ❌ NO GL backing | Migrated loans don't affect interest income; historical interest not backfilled |
| **Balance Sheet** | ⚠️ Partial | Migrated loans don't appear in GL accounts BUT frozen savings are reflected in member accounts |

### Important Note

**Migrated loans are "off-balance-sheet" from a GL perspective** until future repayments occur:
- Repayments ON migrated loans DO create GL entries (Phase 4 rules)
- Only future activity triggers GL accounting
- This is intentional to preserve historical accuracy and avoid GL inconsistencies

**Recommendation**: When reconciling reports after loan migration, be aware that:
- Operational reports (Cashbook, Loan Register) will show migrated loans immediately
- GL-based financial reports (Income Statement, Balance Sheet) will NOT show migrated loan impact until repayments are recorded

---

## Summary: Reducing Balance Alignment

| Aspect | Status | Details |
|--------|--------|---------|
| **Monthly Repayment Calculation** | ✅ YES | Uses proper reducing balance formula |
| **Guarantor Freezing** | ✅ YES | Correctly freezes active loan guarantors |
| **Interest Rate Source** | ✅ YES | Uses product's configured rate (not imported) |
| **Outstanding Balance** | ✅ YES | Treated as snapshot (no recalculation) |
| **Phase 4 Repayment Rules** | ✅ YES | Future repayments follow mandatory split |
| **Historical Interest Import** | ❌ NO | Intentional—old system method unknown |
| **totalInterest Backfill** | ❌ NO | Intentional—only outstandingBalance imported |
| **Interest Recalculation** | ❌ NO | Intentional—preserves historical accuracy |

**Conclusion**: The loan migration system is **partially aligned** with the reducing balance system. It correctly calculates future repayments using reducing balance but intentionally does NOT import or recalculate historical interest to avoid accuracy issues. This is a reasonable design decision given that the old system's interest calculation method is unknown.
