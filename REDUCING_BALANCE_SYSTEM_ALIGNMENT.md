0
.# Reducing Balance System: Implementation Alignment Document

## EXECUTIVE SUMMARY

This document outlines the complete loan lifecycle transformation needed to implement a reducing balance interest system where:
- The treasurer (NOT the system) enters how much of each payment goes to principal vs interest
- The system is a **RECORDING** system, not a CALCULATING system
- Interest is NOT pre-calculated upfront anymore
- Each repayment is manually split by the treasurer into principal and interest components

---

## PART 1: LOAN APPLICATION & APPROVAL PROCESS

### Current System (What EXISTS Now)

**At Loan Application:**
1. Member requests loan: Amount=100,000, Term=12 months, Product Rate=15%
2. **System AUTOMATICALLY calculates:**
   - `totalInterest = 100,000 × 15% × (12/12) = 15,000` (FIXED, never changes)
   - `monthlyRepayment = (100,000 + 15,000) / 12 = 9,583.33`
   - `outstandingBalance = 115,000` (principal + pre-calculated interest)
3. These calculated values are **permanently stored** in the Loan entity

**Approval Workflow:**
- Guarantor Approval → Loan Officer Review → Credit Committee Approval → **Treasurer Final Approval**
- At each stage: Review eligibility, validate guarantors, approve/reject
- **KEY: At PENDING_TREASURER stage, Treasurer enters the interest amount in KES**

**At Treasurer Approval (PENDING_TREASURER → APPROVED):**
- Treasurer sees:
  - Loan Amount: 100,000
  - Product Interest Rate: 15% p.a.
  - Suggested calculation: 100,000 × 15% × 1 = 15,000
- **Treasurer ENTERS:** Total Interest Amount in KES (e.g., 15,000)
- System then calculates:
  - `totalRepayable = 100,000 + 15,000 = 115,000`
  - `monthlyRepayment = 115,000 / 12 = 9,583.33`
  - `outstandingBalance = 115,000`
- Loan status → APPROVED
- UI shows: "Final Approval - Set Interest Rate & Confirm Disbursement"

**At Disbursement (Treasurer only):**
- Treasurer clicks "Disburse" on APPROVED loans
- Member receives: 100,000 KES
- `outstandingBalance` = 115,000 (principal + interest)
- Loan status → DISBURSED

### Key Current Behavior

- ✅ **Treasurer enters the total interest amount manually at approval time**
- ✅ This interest amount is the TOTAL for the entire loan term
- ✅ Interest is pre-calculated as a fixed total (not reducing balance)
- ✅ Treasurer confirms and then performs disbursement
- ✅ Backend code: `LoanService.approveLoan()` receives `interestRate` field (actually KES amount, not percentage)

### Problem with Current System

The treasurer manually enters a FIXED interest amount upfront:
- "Total interest for this loan will be 15,000 KES"
- But with reducing balance method, total interest is UNKNOWN upfront
- Interest changes monthly based on remaining principal
- Treasurer cannot predict total interest when balance reduces each month

---

## PART 2: WHAT CHANGES IN NEW SYSTEM

### Loan Application (NO CHANGE)

**At Loan Application:**
1. Member requests loan: Amount=100,000, Term=12 months, Product Rate=15%
2. **System stores ONLY:**
   - `amount` = 100,000 (principal)
   - `interestRate` = 15% (from product)
   - `termMonths` = 12
   - `outstandingBalance` = 100,000 (ONLY the principal, NOT principal + interest)
3. **System does NOT calculate:**
   - ❌ `totalInterest` (not calculated upfront anymore)
   - ❌ `monthlyRepayment` (not fixed anymore)
   - ❌ `totalRepayable` (not needed)

**Approval Workflow (NO CHANGE):** 
- Same as before (Guarantor → Officer → Committee → Treasurer)

### Treasurer Approval (PENDING_TREASURER → APPROVED) - THE CHANGE

**Current behavior (what we're removing):**
- Treasurer ENTERS: Total Interest Amount in KES (e.g., 15,000)
- System calculates: `totalRepayable` and `monthlyRepayment` from the interest entered
- Outstanding balance set to: 115,000 (principal + interest)
- UI shows: "Final Approval - Set Interest Rate & Confirm Disbursement"

**New behavior (reducing balance):**
- Treasurer NO LONGER ENTERS interest at approval
- Treasurer only CONFIRMS: "Approve for disbursement"
- System NO LONGER calculates: `totalInterest`, `monthlyRepayment`, `totalRepayable`
- Outstanding balance = 100,000 (principal only)
- UI change: Remove interest input field from PENDING_TREASURER approval dialog

### At Disbursement (Treasurer action)
- Treasurer clicks "Disburse"
- Member receives: 100,000 KES
- `outstandingBalance` = 100,000 (principal only)
- Loan status → DISBURSED

### Key Changes at Approval Stage

- ❌ **REMOVE:** Treasurer entering total interest amount at approval
- ❌ **REMOVE:** System pre-calculating `totalInterest`, `monthlyRepayment`, `totalRepayable`
- ✅ **CHANGE:** Outstanding balance starts as 100,000 (principal only)
- ✅ **CHANGE:** Interest will be determined later during repayments based on reducing balance

---

## PART 3: LOAN REPAYMENT (THE CORE CHANGE)

### Current System (What EXISTS Now)

**Individual Repayment Recording (UI: LoanRepaymentRecording.tsx):**

Treasurer enters ONLY:
- Total amount (e.g., 9,583.33)
- Payment method (Cash, M-Pesa, etc.)
- Reference number (loan number)
- Payment date

Backend (`LoanRepaymentService.recordRepayment()`) does:
```java
// Current logic
BigDecimal principal = principalAmount != null ? principalAmount : BigDecimal.ZERO;
BigDecimal interest = interestAmount != null ? interestAmount : BigDecimal.ZERO;

// If neither provided, assumes ALL goes to principal, ZERO to interest
if (principal == ZERO && interest == ZERO) {
  principal = amount;  // All to principal
  interest = ZERO;     // Nothing to interest
}

// Validation: principal + interest must = amount
if (principal + interest != amount) {
  throw error;
}

// Updates outstanding balance
newOutstandingBalance = outstanding - amount;
```

**Problem:** 
- Treasurer never enters principal/interest split
- Backend assumes everything goes to principal
- Interest amount is NOT recorded accurately for each payment
- GL accounting doesn't know how much interest was earned from each payment

### New System (What WE'RE BUILDING)

**Individual Repayment Recording (UI: LoanRepaymentRecording.tsx - ENHANCED):**

Treasurer MUST enter BOTH:
1. **Total repayment amount** (e.g., 9,583.33)
2. **Principal amount** (e.g., 8,286.34) — how much reduces the loan principal
3. **Interest amount** (calculated or entered) — how much is SACCO's profit

Form structure:
```
Loan: L-001
Outstanding Balance: 100,000

Enter Repayment Split:
┌─ Total Amount: [9,583.33] (required)
├─ Principal Amount: [8,286.34] (required - treasurer enters)
└─ Interest Amount: [auto-calculated as 9,583.33 - 8,286.34 = 1,297] (read-only)

Validation: Principal + Interest MUST = Total Amount
```

**Backend behavior (LoanRepaymentService.recordRepayment() - NO CHANGE NEEDED):**
```java
// Backend already handles this correctly
BigDecimal principal = principalAmount;  // From UI
BigDecimal interest = interestAmount;    // From UI (or calculated)

// Validation already in place
if (principal + interest != amount) {
  throw error;  // Prevents invalid splits
}

// Updates outstanding balance correctly
newOutstandingBalance = outstanding - principal;  // Only principal reduces balance
// Interest is recorded separately in loan_repayments.interest_amount
```

**Key Points:**
- ✅ Backend already supports principal/interest split (columns exist in DB)
- ✅ Backend already validates principal + interest = total
- ✅ Backend already updates outstanding balance by reducing principal only
- ✅ Backend already posts interest to GL accounts
- 🔧 Frontend NEEDS to be updated to provide these input fields

---

## PART 4: VALIDATION DURING REPAYMENT

### What Gets Validated

**Validation 1: Amount is positive**
```
amount > 0 ✓
```

**Validation 2: Amount doesn't exceed outstanding**
```
amount ≤ outstandingBalance ✓
```

**Validation 3: Principal + Interest = Total**
```
principalAmount + interestAmount = amount ✓
```

**Validation 4: Neither can be negative**
```
principalAmount ≥ 0 ✓
interestAmount ≥ 0 ✓
```

### Accuracy of Field Population

**What happens after recording:**

Example: Repayment of 9,583.33 split as:
- Principal: 8,286.34
- Interest: 1,297

```
BEFORE:
├─ Loan.outstandingBalance = 100,000
├─ Loan.interestRemaining = (old system tracking, decreasing)
└─ Status = DISBURSED

AFTER:
├─ Loan.outstandingBalance = 100,000 - 8,286.34 = 91,713.66 ✓ (CORRECT)
├─ LoanRepayment.principalAmount = 8,286.34 ✓ (stored)
├─ LoanRepayment.interestAmount = 1,297 ✓ (stored)
├─ LoanRepayment.amount = 9,583.33 ✓ (stored)
├─ Transaction.INTEREST = 1,297 ✓ (GL posts interest)
└─ Status = still DISBURSED (or REPAID if balance = 0)
```

### Next Month's Calculation

When treasurer records 2nd repayment 30 days later:
- Outstanding balance is NOW 91,713.66 (not original 100,000)
- Interest calculation (if they knew it) would be based on NEW balance
- Reducing balance method is automatic because balance decreased

---

## PART 5: BULK PROCESSING (EXCEL UPLOADS) - MONTHLY CONTRIBUTIONS & PAYMENT METHODS

### Current System (What EXISTS Now)

**Monthly Contributions Page (BulkProcessing.tsx):**
- Treasurer uploads Excel file with member contributions for the month
- Frontend generates template with these visible columns:
  ```
  Employee ID | Savings | Loan Repayment Principal Amount | Loan Repayment Interest Amount | Loan Repayment | Loan Number | [Custom Funds...] |
  ```

**Current Excel Template From Frontend:**
```
Employee ID | Savings | Loan Repayment Principal Amount | Loan Repayment Interest Amount | Loan Repayment | Loan Number | Benevolent Fund | ...
EMP001      | 5,000   | (empty)                          | (empty)                        | (empty)         | (empty)     | (empty)         |
EMP002      | 8,000   | 3,500                            | 1,500                          | 5,000           | LN-2026-002 | (empty)         |
EMP003      | 6,000   | (empty)                          | (empty)                        | (empty)         | (empty)     | (empty)         |
```

**Backend Parser (ExcelParserService.parseMonthlyContributions):**
- Reads columns in this EXACT order (column-based):
  - Col 0: Employee ID (required)
  - Col 1: Savings 
  - Col 2: Shares (skipped by frontend, but backend still expects it)
  - Col 3: Loan Repayment (total)
  - Col 4: Loan Number
  - Col 5: Loan Repayment Principal Amount (optional)
  - Col 6: Loan Repayment Interest Amount (optional)
  - Col 7: **Loan Repayment Payment Method** ← SUPPORTED by backend but MISSING from frontend template
  - Col 8: Loan Repayment Reference Number (optional)
  - Col 9+: Dynamic fund columns

**CRITICAL ISSUE IDENTIFIED:**
- ✅ **Backend ALREADY supports Payment Method column** (Column 7 in ExcelParserService)
- ✅ **Backend defaults to SALARY_DEDUCTION if column is missing**
- ❌ **Frontend template download DOES NOT include Payment Method column**
- ❌ **Treasurer has no way to specify payment method** (CASH, MPESA, etc.) when uploading
- ❌ **All bulk repayments are assumed to be SALARY by default** (which is realistic but should be explicit)

**Processing in BulkProcessingService:**
1. Parse Excel rows using ExcelParserService
2. For each loan repayment row:
   - Reads: Employee ID, Loan #, Principal Amount, Interest Amount, Total Repayment
   - **Attempts to read:** Payment Method from Column 7 (but template doesn't include it)
3. If Payment Method column is missing from Excel → defaults to SALARY_DEDUCTION
4. No way for treasurer to specify CASH, MPESA, BANK_TRANSFER, CHEQUE repayments in bulk uploads

### New System (What WE'RE BUILDING)

**Enhanced Excel Template for Monthly Contributions (Frontend Generation):**

Current (from frontend - INCOMPLETE):
```
Employee ID | Savings | Loan Repayment Principal Amount | Loan Repayment Interest Amount | Loan Repayment | Loan Number | Benevolent Fund | ...
EMP001      | 5,000   | (empty)                          | (empty)                        | (empty)         | (empty)     | (empty)         |
EMP002      | 8,000   | 3,500                            | 1,500                          | 5,000           | LN-2026-002 | (empty)         |
EMP003      | 6,000   | (empty)                          | (empty)                        | (empty)         | (empty)     | (empty)         |
```

**New (with Payment Method added):**
```
Employee ID | Savings | Loan Repayment Principal Amount | Loan Repayment Interest Amount | Loan Repayment | Loan Number | Loan Repayment Payment Method | Benevolent Fund | ...
EMP001      | 5,000   | (empty)                          | (empty)                        | (empty)         | (empty)     | SALARY                        | (empty)         |
EMP002      | 8,000   | 3,500                            | 1,500                          | 5,000           | LN-2026-002 | SALARY                        | (empty)         |
EMP003      | 6,000   | 4,200                            | 800                            | 5,000           | LN-2026-003 | CASH                          | (empty)         |
```

**Changes Required:**

1. **Frontend Template Enhancement (BulkProcessing.tsx - downloadTemplate function)**
   - Add "Loan Repayment Payment Method" column to MONTHLY_CONTRIBUTIONS template
   - Position: After "Loan Number" column (before fund columns)
   - Default value in template: "SALARY"
   - Example row with loan: set payment method to "SALARY"
   - Example row without loan: set payment method to "SALARY" (for consistency, even though unused)
   - Update template data structure:
     ```typescript
     const baseRow = {
       "Employee ID": "EMP001",
       "Savings": 5000,
       "Loan Repayment Principal Amount": "",
       "Loan Repayment Interest Amount": "",
       "Loan Repayment": "",
       "Loan Number": "",
       "Loan Repayment Payment Method": "SALARY",  // ← ADD THIS
       ...fundColumns,
     };
     ```

2. **Backend Validation (ExcelParserService - Already Supported)**
   - ✅ ExcelParserService.parseMonthlyContributions() already reads Column 7 (Payment Method)
   - ✅ Already defaults to SALARY_DEDUCTION if column is missing
   - No code changes needed in parser - just frontend template update

3. **Backend Validation Rules (BulkValidationService - to be Enhanced):**
   ```
   For each row with a loan repayment:
   ✓ Employee ID exists and matches member
   ✓ Loan Number exists and belongs to member
   ✓ Loan Repayment Amount > 0
   ✓ Loan Repayment Principal Amount provided (NOT empty)
   ✓ Loan Repayment Interest Amount provided (NOT empty)
   ✓ Loan Repayment Payment Method provided (NOT empty) ← ADD THIS CHECK
   ✓ Principal + Interest = Total Repayment Amount
   ✓ Total Repayment Amount ≤ Outstanding Balance
   ✓ Payment Method is one of: SALARY, CASH, MPESA, BANK_TRANSFER, CHEQUE, OTHER
   ```

4. **Processing Logic (BulkProcessingService - No Changes Needed)**
   ```
   For each valid row with loan repayment:
   ├─ Extract: Employee ID, Loan ID, Total, Principal, Interest, Payment Method
   ├─ ExcelParser already extracts payment method from Column 7
   ├─ BulkTransactionItem.loanRepaymentPaymentMethod already populated
   ├─ Call: LoanRepaymentService.recordRepayment(
   │  ├─ loanId
   │  ├─ amount: Total Repayment
   │  ├─ principalAmount: Loan Repayment Principal Amount
   │  ├─ interestAmount: Loan Repayment Interest Amount
   │  ├─ paymentMethod: From Excel column ← NOW PROPERLY PASSED
   │  └─ ...other fields
   └─ Record transaction with payment method tagged
   ```

5. **Summary of What Needs to Change**
   - ✅ **Backend Parser:** Already supports Column 7 (SALARY_DEDUCTION default)
   - ✅ **Database:** BulkTransactionItem.loanRepaymentPaymentMethod already exists
   - ✅ **Backend Processing:** Already reads and uses payment method
   - ❌ **Frontend Template:** MUST add "Loan Repayment Payment Method" column to generated Excel
   - ❌ **Frontend Validation:** Should show/hint that SALARY is most common default
   - ❌ **Backend Validation:** Should validate payment method is provided (not just optional)
   │  ├─ paymentMethod: Loan Repayment Payment Method (from Excel) ← NEW
   │  ├─ referenceNumber: (if provided)
   │  └─ paymentDate: (date of bulk upload or specified date)
   │)
   └─ Record Transaction with payment method
   ```

### Bulk Processing Data Accuracy

**Current State (Gap in Frontend Only):**
```
Excel row (MISSING Payment Method column): EMP002 | 8000 | 3,500 | 1,500 | 5,000 | LN-2026-002 | (no column exists)
    ↓
System records:
├─ principal_amount = 3,500 ✓
├─ interest_amount = 1,500 ✓
├─ payment_method = "SALARY_DEDUCTION" (DEFAULTED, not from Excel)
├─ Outstanding balance = 100,000 - 3,500 = 96,500 ✓
├─ GL post: Interest income = 1,500 ✓
└─ Audit: Records don't show what payment method was actually used

ISSUE: 
- Treasurer cannot specify payment method in Excel
- System defaults to SALARY for all bulk uploads
- Works for 95% of cases (salary deductions are most common)
- But impossible to upload CASH or MPESA repayments in bulk
```

**New State (After Adding Frontend Template Column):**
```
Excel row (WITH Payment Method column): EMP002 | 8000 | 3,500 | 1,500 | 5,000 | LN-2026-002 | SALARY
    ↓
System records:
├─ principal_amount = 3,500 ✓
├─ interest_amount = 1,500 ✓
├─ payment_method = "SALARY" ✓ (FROM EXCEL, explicitly entered)
├─ Outstanding balance = 100,000 - 3,500 = 96,500 ✓
├─ GL post: Interest income = 1,500 ✓
└─ Audit trail: Shows payment was via SALARY ✓

BENEFIT: Payment method is now explicit, recorded from Excel, and auditable
```

**Example 2 - Non-Salary Payment in Bulk:**
```
Excel row (Bulk upload with CASH): EMP003 | 6000 | 4,200 | 800 | 5,000 | LN-2026-003 | CASH
    ↓
System records:
├─ principal_amount = 4,200 ✓
├─ interest_amount = 800 ✓
├─ payment_method = "CASH" ✓ (from Excel, explicitly entered)
├─ Outstanding balance = 95,000 - 4,200 = 90,800 ✓
├─ GL post: Interest income = 800 ✓
└─ Audit trail: Shows payment was manually collected in CASH ✓

BENEFIT: Now supports non-salary bulk repayments (cash collections, M-Pesa transfers, etc.)
```

### Payment Methods in System

**Available Payment Methods:**
- **SALARY** (most common - treasurer can set as template default)
- **CASH** (manual office collection)
- **MPESA** (M-Pesa mobile money payment)
- **BANK_TRANSFER** (direct bank transfer)
- **CHEQUE** (cheque payment)
- **OTHER** (miscellaneous)

**Why SALARY is Most Common:**
- Most employers run automatic payroll deductions
- Members have salary cuts directly transferred to SACCO
- Bulk uploads handle entire month's salary-deducted contributions
- Eliminates collection risk and manual handling

**Stored In System:**
- `BulkTransactionItem.loanRepaymentPaymentMethod` (already exists in DB)
- `LoanRepayment.paymentMethod` (already exists in DB)
- Transaction records for audit trail

**Why It Matters:**
- ✓ Audit trail: Know HOW each payment was made
- ✓ Tracking: Understand payment channels used
- ✓ Reporting: Analyze payment methods by member/loan/month/channel
- ✓ GL Posting: Can correlate to relevant GL accounts if needed
- ✓ Reconciliation: Match bulk uploads to actual payment sources
- ✓ Member records: Show on statements how payments were recorded

---

## PART 6: LOAN MIGRATIONS (FROM OLD SYSTEM)

### Current System (What EXISTS Now)

**Old SACCO data imported via LoanMigrationService:**
```
Loan from old system:
├─ Principal: 100,000 (original)
├─ Interest Rate: 15% p.a.
├─ Term: 12 months
├─ Outstanding Balance: 95,000 (amount still owed - mix of principal + accrued interest)
└─ Total Interest: (some value calculated by old system)
```

**During Migration:**
1. System imports outstanding balance as-is: 95,000
2. System imports total interest from old system
3. These become fixed values in the Loan record
4. Loan is marked as `migrationStatus = MIGRATED`

**Problem:** 
- Outstanding balance is mixture of principal and interest
- We don't know the exact split
- Total interest is historical and doesn't match reducing balance method

### New System (What WE'RE BUILDING)

**Old SACCO data imported via LoanMigrationService (ADJUSTED):**
```
Loan from old system:
├─ Principal: 100,000 (original)
├─ Interest Rate: 15% p.a.
├─ Term: 12 months
├─ Outstanding Balance: 95,000 (keep as-is, starting point)
└─ Total Interest: (DO NOT pre-calculate or import)
```

**During Migration (NO CHANGE in import process):**
1. System imports outstanding balance: 95,000
2. ~~System imports total interest from old system~~ ← REMOVE THIS
3. System stores: principal, rate, term, outstanding balance
4. Loan marked as `migrationStatus = MIGRATED`

**After Migration - When First Repayment Comes In:**

Treasurer records repayment of, say, 5,000:
```
Outstanding: 95,000
Treasurer enters:
├─ Total Repayment: 5,000
├─ Principal: 4,500 (treasurer decides)
└─ Interest: 500 (treasurer decides)

System records:
├─ Loan.outstandingBalance = 95,000 - 4,500 = 90,500
├─ LoanRepayment.principal = 4,500
├─ LoanRepayment.interest = 500
└─ GL posts interest: 500
```

**Key Changes:**
- ✅ Outstanding balance is used as starting point (not separated)
- ✅ NO pre-calculated total interest from old system
- ✅ Each new repayment is split manually by treasurer
- ✅ Future calculations work on new outstanding balance
- ✅ Historical interest calculation method doesn't matter anymore

### Migration Functionality Preserved

```
✓ Loan data imports correctly
✓ Outstanding balance is maintained
✓ Repayment history continues
✓ GL accounts post new interest correctly
✓ Reducing balance method applies to ALL future repayments (new and migrated)
✓ Audit trail shows when loan was migrated
```

---

## PART 7: DATABASE & BACKEND INFRASTRUCTURE

### Existing Database Support

**loan_repayments table (ALREADY HAS these columns):**
```sql
id
loan_id
amount (total)
principal_amount ← ALREADY EXISTS ✓
interest_amount ← ALREADY EXISTS ✓
payment_method
reference_number
payment_date
created_at
recorded_by
```

**loans table (ALREADY HAS these columns):**
```sql
id
amount (original principal)
interest_rate
term_months
outstanding_balance ← Already updated correctly ✓
status
disbursement_date
member_id
...
```

**NO DATABASE MIGRATIONS NEEDED** - structure already supports reducing balance recording

### Backend Services (What EXISTS vs. What Needs Change)

| Service | Method | Current State | Change Needed |
|---------|--------|---------------|---------------|
| LoanService | createLoan() | ❌ Calculates totalInterest upfront | ✅ Remove totalInterest calculation |
| LoanService | approveLoan() | ❌ **Treasurer enters & calculates** totalInterest, monthlyRepayment, totalRepayable | ✅ **REMOVE all interest calculations** - just approve for disbursement |
| LoanRepaymentService | recordRepayment() | ✅ Accepts principal/interest split | No change (already correct) |
| BulkProcessingService | processBulkUpload() | ❌ Makes principal/interest optional | ✅ Make both mandatory |
| LoanMigrationService | migrateLoan() | ❌ Imports totalInterest | ✅ Skip totalInterest import |

### Frontend Component Changes

| Component | Current | Change Needed |
|-----------|---------|---------------|
| LoanRepaymentRecording.tsx | Only total amount input | ✅ Add principal & interest inputs |
| Loans.tsx | Has interest input at PENDING_TREASURER | ✅ Remove interest input field from approval dialog |
| BulkProcessing.tsx | Optional split columns, no payment method column in template | ✅ Add "Loan Repayment Payment Method" column to template download |

---

## PART 8: KEY BEHAVIORS & GUARANTEES

### Behavior 1: Principal vs Interest Split

**Guaranteed:**
```
principal_amount + interest_amount = total_repayment_amount

✓ Validated at backend
✓ Validated at frontend
✓ Cannot be bypassed
```

### Behavior 2: Outstanding Balance Reduction

**Guaranteed:**
```
new_outstanding_balance = old_outstanding_balance - principal_amount

NOT: old_outstanding_balance - total_amount
NOT: old_outstanding_balance - interest_amount

✓ Only principal reduces the loan
✓ Interest is SACCO's profit (goes to GL income)
```

### Behavior 3: Interest Recording

**Guaranteed:**
```
For each LoanRepayment:
├─ principal_amount → Reduces loan balance
├─ interest_amount → Posted to GL interest income account
└─ Both amounts stored → For reporting & audit

✓ SACCO can report total interest collected
✓ GL accounts are accurate
✓ Audit trail shows split for each payment
```

### Behavior 4: Reducing Balance Effect

**Guaranteed (Natural Outcome):**
```
Month 1 Repayment:
  Outstanding: 100,000
  If Treasurer enters interest as 1,297, outstanding becomes 98,703

Month 2 Repayment (30 days later):
  Outstanding: 98,703 (REDUCED from month 1)
  If Treasurer calculates interest on 98,703, it will be LESS than 1,297
  
Month 3 Repayment:
  Outstanding: further reduced
  Interest will be even less
  
✓ Interest naturally decreases because outstanding balance decreases
✓ No system automation needed - just accurate recording of what treasurer enters
```

---

## PART 9: MIGRATION CHECKLIST

### Code Changes Required

```
FRONTEND:
  ☐ LoanRepaymentRecording.tsx
    ☐ Add principalAmount input field
    ☐ Add interestAmount input field
    ☐ Show total = principal + interest
    ☐ Validate principal + interest = total
    ☐ Pass both to backend API

  ☐ BulkProcessing.tsx (downloadTemplate function - MONTHLY_CONTRIBUTIONS case)
    ☐ Add "Loan Repayment Payment Method" column after "Loan Number"
    ☐ Set default value: "SALARY" in all example rows
    ☐ Position: After "Loan Number", before fund columns
    ☐ Update baseRow, rowWithLoan, rowWithoutLoan objects

BACKEND:
  ☐ LoanService.createLoan()
    ☐ Remove totalInterest calculation
    ☐ Remove monthlyRepayment calculation
    ☐ Remove totalRepayable calculation
    ☐ Set outstandingBalance = amount (NOT amount + interest)

  ☐ LoanRepaymentService.recordRepayment()
    ☐ No changes (already correct) ✓

  ☐ BulkValidationService
    ☐ Add validation: If loan repayment exists, payment method must be populated
    ☐ Add validation: Payment method is one of allowed values

  ☐ LoanMigrationService.migrateLoan()
    ☐ Skip importing totalInterest
    ☐ Keep outstanding balance as-is

  ☐ ExcelParserService
    ☐ No changes (already reads Column 7 payment method) ✓

API CHANGES:
  ☐ POST /loans/{loanId}/repay
    ☐ Already accepts principalAmount & interestAmount ✓
    ☐ No changes needed
```

### Database Changes Required

```
NONE - existing schema supports it all ✓
```

### Testing Required

```
☐ Single repayment recording with split
☐ Bulk upload with mandatory splits
☐ Outstanding balance reduces correctly
☐ GL posting is accurate
☐ Migrated loans work with new splits
☐ Interest decreases each month (for manual verification)
☐ Audit trail shows splits
```

---

## PART 10: SUMMARY OF UNDERSTANDING

### System Role: RECORDING, NOT CALCULATING

| Action | Who | System Role |
|--------|-----|------------|
| Decide principal/interest split | **Treasurer** | Record it |
| Validate split accuracy | **System** | Check principal + interest = total |
| Store split | **System** | Store both amounts |
| Calculate interest upfront | **Nobody (removed)** | No longer exists |
| Calculate future interest | **Treasurer** | Manual decision per payment |
| Post to GL | **System** | Use interest_amount from record |

### What Changes

1. **At Loan Application:** No more pre-calculated interest
2. **At Disbursement:** Outstanding balance = principal only (not principal + interest)
3. **At Repayment:** Treasurer must enter principal AND interest split
4. **At Bulk Upload:** Principal/interest splits become mandatory
5. **At Migration:** No pre-calculated interest imported from old system

### What Stays the Same

1. **Database structure:** All tables already support it
2. **Backend validation:** Already in place
3. **GL posting:** Already uses interest_amount correctly
4. **Audit trail:** Already recorded

---

---

## PART 11: COMPLETE BEFORE/AFTER FLOW SUMMARY

### CURRENT SYSTEM FLOW (What Exists Today)

```
LOAN APPLICATION
├─ Member requests: 100,000 KES, 12 months, 15% rate
├─ System auto-calculates:
│  ├─ Total Interest: 15,000 KES (FIXED)
│  ├─ Total Repayable: 115,000 KES
│  ├─ Monthly Payment: 9,583.33 KES
│  └─ Outstanding Balance: 115,000 KES
└─ Stored in Loan entity (never changes)

APPROVAL WORKFLOW
├─ Guarantor Approval
├─ Loan Officer Review
├─ Credit Committee Approval
└─ TREASURER APPROVAL (PENDING_TREASURER)
   └─ **Treasurer ENTERS: Total Interest Amount (e.g., 15,000 KES)**
      ├─ System validates it matches calculation
      ├─ System sets: totalRepayable = amount + interest
      ├─ System sets: monthlyRepayment = totalRepayable / term
      ├─ System sets: outstandingBalance = totalRepayable (115,000)
      └─ Loan Status → APPROVED

DISBURSEMENT (Treasurer action)
├─ Loan Status: APPROVED
├─ Treasurer clicks "Disburse"
├─ Member receives: 100,000 KES
├─ Outstanding Balance: 115,000 KES (STARTS AS PRINCIPAL + INTEREST)
└─ Loan Status → DISBURSED

REPAYMENT (Treasurer records each month)
├─ Treasurer enters ONLY: Total amount (e.g., 9,583.33)
├─ System assumes: All goes to principal, ZERO to interest
├─ Outstanding Balance updated: 115,000 - 9,583.33 = 105,416.67
└─ Interest tracking: BROKEN (not recorded per payment)
```

### NEW SYSTEM FLOW (After Implementation)

```
LOAN APPLICATION
├─ Member requests: 100,000 KES, 12 months, 15% rate
├─ System stores ONLY:
│  ├─ Principal Amount: 100,000 KES
│  ├─ Interest Rate: 15% p.a.
│  ├─ Term: 12 months
│  └─ Outstanding Balance: 100,000 KES (PRINCIPAL ONLY)
└─ No pre-calculations (totalInterest, monthlyRepayment removed)

APPROVAL WORKFLOW
├─ Guarantor Approval
├─ Loan Officer Review
├─ Credit Committee Approval
└─ TREASURER APPROVAL (PENDING_TREASURER)
   └─ **Treasurer NO LONGER ENTERS interest amount**
      ├─ Treasurer only CONFIRMS: "Approve for disbursement"
      ├─ No interest entry field in UI
      ├─ System does NOT calculate anything
      ├─ Outstanding Balance STAYS: 100,000 KES (principal only)
      └─ Loan Status → APPROVED

DISBURSEMENT (Treasurer action)
├─ Loan Status: APPROVED
├─ Treasurer clicks "Disburse"
├─ Member receives: 100,000 KES
├─ Outstanding Balance: 100,000 KES (PRINCIPAL ONLY)
└─ Loan Status → DISBURSED

REPAYMENT (Treasurer records each month - ENHANCED)
├─ Month 1 (30 days elapsed):
│  ├─ Outstanding Balance: 100,000 KES
│  ├─ Treasurer ENTERS BOTH:
│  │  ├─ Total Amount: 9,583.33 KES
│  │  ├─ Principal Amount: 8,286.34 KES (treasurer decides)
│  │  └─ Interest Amount: 1,296.99 KES (calculated as Total - Principal)
│  ├─ System validates: Principal + Interest = Total ✓
│  ├─ Outstanding Balance updated: 100,000 - 8,286.34 = 91,713.66
│  ├─ Interest recorded: 1,296.99 (for GL posting)
│  └─ Loan Status → Still DISBURSED
│
├─ Month 2 (30 days elapsed):
│  ├─ Outstanding Balance: 91,713.66 (REDUCED, not original 100,000)
│  ├─ If treasurer calculates (or is guided): Interest should be ~1,195 (less than month 1)
│  ├─ Treasurer ENTERS BOTH:
│  │  ├─ Total Amount: 9,500 KES
│  │  ├─ Principal Amount: 8,300 KES (treasurer decides)
│  │  └─ Interest Amount: 1,200 KES
│  ├─ Outstanding Balance updated: 91,713.66 - 8,300 = 83,413.66
│  ├─ Interest recorded: 1,200
│  └─ REDUCING BALANCE naturally happens (balance decreases each month)
│
└─ Continues until Outstanding Balance = 0
```

### KEY DIFFERENCES

| Aspect | Current System | New System |
|--------|---|---|
| **Interest at Application** | Auto-calculated | Not calculated |
| **Interest at Approval** | **Treasurer enters total** | **Removed - no entry** |
| **Interest Tracking** | Broken/incorrect | **Recorded per payment** |
| **Outstanding Balance Start** | principal + interest | **Principal only** |
| **Monthly Interest** | Fixed same amount | **Decreases as balance reduces** |
| **Reducing Balance Effect** | Not implemented | **Automatic (natural result)** |
| **Treasurer Role at Approval** | Enters interest amount | **Only confirms approval** |
| **Treasurer Role at Repayment** | Enters total amount only | **Enters total + principal + interest** |
| **Validation** | Only basic checks | **Principal + Interest = Total** |

---

## CONFIRMATION NEEDED

Please confirm this understanding is correct based on the research:

**TREASURER'S ROLE - BEFORE vs. AFTER:**

1. ✅ **At Approval (PENDING_TREASURER stage):**
   - **CURRENT:** Treasurer MUST enter "Total Interest Amount in KES" (e.g., 15,000)
   - **FUTURE:** Treasurer NO LONGER enters interest; only confirms "Approve for disbursement"

2. ✅ **At Disbursement:**
   - **CURRENT:** Treasurer clicks "Disburse" with pre-calculated interest (no change needed)
   - **FUTURE:** Same action, but outstanding balance = principal only (not principal + interest)

3. ✅ **At Repayment:**
   - **CURRENT:** Treasurer enters only total amount
   - **FUTURE:** Treasurer enters total amount + principal amount + interest amount

**SYSTEM CHANGES:**

4. ✅ LoanService.approveLoan() must remove the interest calculation logic
5. ✅ Treasurer approval UI (Loans.tsx) must remove interest input field
6. ✅ LoanRepaymentRecording.tsx must add principal and interest input fields
7. ✅ BulkProcessingService must require (not optional) principal/interest columns
8. ✅ LoanMigrationService must skip importing totalInterest
9. ✅ Database already supports this (no schema changes needed)

**IF ALL CORRECT:** Ready to proceed with implementation.
**IF ANY CORRECTIONS NEEDED:** Please specify which points need adjustment.
