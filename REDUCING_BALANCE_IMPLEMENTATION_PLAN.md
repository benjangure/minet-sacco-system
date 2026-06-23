# Reducing Balance Interest Implementation Plan

## THE PROBLEM
Currently, the system doesn't properly track how much of each repayment goes to **principal (reduces loan balance)** vs **interest (SACCO's profit)**. With reducing balance method, this split changes every month based on remaining balance.

**Why it matters:**
- SACCO's profit = Interest collected
- Accurate reporting depends on knowing exactly how much interest was earned
- GL accounting needs to post interest to income account
- Member needs to know principal reduction vs interest paid

---

## WHAT WE'RE BUILDING

A system where when treasurer records a repayment, they can see:

```
Loan: L-001 (Outstanding: 105,000)

System shows (based on reducing balance):
├─ Expected Interest for 30 days: 1,296.99
├─ Suggested Principal: 8,286.34
└─ Total: 9,583.33

Treasurer can:
✓ Accept suggestion
✓ Override with their own split
✓ System records what they entered

Result recorded:
├─ Principal: 8,286.34 → Goes to principal reduction
├─ Interest: 1,296.99 → SACCO's profit (GL income account)
└─ Total: 9,583.33 → Outstanding reduces by this amount
```

---

## IMPLEMENTATION ROADMAP

### PHASE 1: Backend - Interest Calculator Service
**What:** Create service to calculate expected interest based on reducing balance

**Location:** New file
```
backend/src/main/java/com/minet/sacco/service/
  └─ ReducingBalanceInterestCalculator.java
```

**What it does:**
```java
calculateExpectedInterest(
    Loan loan,
    LocalDate lastPaymentDate,      // When was last payment
    LocalDate currentPaymentDate    // When is this payment
) → {
    interestAmount: 1,296.99
    suggestedPrincipal: 8,286.34
    daysElapsed: 30
}
```

**Formula:**
```
Interest = Outstanding Balance × (Annual Rate / 365) × Days Since Last Payment

Example:
Interest = 105,000 × (15% / 365) × 30
         = 105,000 × 0.0411% × 30
         = 1,296.99
```

**Key inputs needed:**
- `loan.outstandingBalance` - Current amount owed
- `loan.interestRate` - Annual rate from product
- Last payment date (from repayment history or disbursement date if first payment)
- Current payment date

---

### PHASE 2: Backend - API Endpoint for Guidance
**What:** New endpoint that returns expected interest before recording repayment

**Endpoint:**
```
GET /api/loans/{loanId}/expected-repayment-split?paymentDate=2026-06-18

Response:
{
  "outstandingBalance": 105000.00,
  "expectedInterest": 1296.99,
  "suggestedPrincipal": 8286.34,
  "totalRepayment": 9583.33,
  "daysElapsed": 30,
  "lastPaymentDate": "2026-05-19"
}
```

**Location:** Add method to `LoanController.java`

**Implementation:**
```java
@GetMapping("/{loanId}/expected-repayment-split")
public ResponseEntity<?> getExpectedRepaymentSplit(
    @PathVariable Long loanId,
    @RequestParam LocalDate paymentDate
) {
    Loan loan = loanService.getLoanById(loanId);
    LocalDate lastPaymentDate = getLastPaymentDate(loanId);
    
    InterestCalculation calc = interestCalculator.calculateExpectedInterest(
        loan, 
        lastPaymentDate, 
        paymentDate
    );
    
    return ResponseEntity.ok(calc);
}
```

---

### PHASE 3: Backend - Update Repayment Recording

**Current method signature (STAYS THE SAME):**
```java
public LoanRepayment recordRepayment(
    Long loanId,
    BigDecimal amount,              // Total being paid
    BigDecimal principalAmount,     // How much goes to principal
    BigDecimal interestAmount,      // How much goes to interest
    LoanRepayment.PaymentMethod paymentMethod,
    String referenceNumber,
    LocalDateTime paymentDate,
    User recordedBy
)
```

**What changes:**
- Validation now expects `principal + interest = total` ✅ (already does this)
- Accept whatever treasurer enters (no forced calculation) ✅ (already does this)
- Add logging to track if split matches expected value ✅ (new)
- Ensure GL posting uses the interest amount correctly ✅ (check existing)

**Code location:** `LoanRepaymentService.recordRepayment()`

**Key validation to verify:**
```java
// This must already work:
if (principalAmount.add(interestAmount).compareTo(amount) != 0) {
    throw new RuntimeException("Principal + Interest must equal total amount");
}
```

---

### PHASE 4: Frontend - UI Enhancement

**File:** `LoanRepaymentRecording.tsx`

**Current form (missing guidance):**
```
┌─ Amount: [9,583.33]
├─ Payment Method: [Dropdown]
└─ Payment Date: [Date picker]
```

**New form (with guidance):**
```
Outstanding Balance: 105,000.00
Last Payment: 2026-05-19 (30 days ago)

┌─ Recommended Based on Reducing Balance:
│  ├─ Expected Interest: 1,296.99
│  └─ Suggested Principal: 8,286.34
│
├─ Your Repayment:
│  ├─ Total Amount: [9,583.33]    (treasurer enters)
│  ├─ Principal: [8,286.34]       (treasurer enters or accepts suggestion)
│  └─ Interest: [1,296.99]        (auto-calculates as Total - Principal)
│
├─ Payment Method: [Dropdown]
└─ Payment Date: [Date picker]

[Status indicator: ✓ Matches recommendation / ⚠ Different from recommendation]
```

**Implementation steps:**

1. **Fetch expected interest when loan selected:**
```typescript
const fetchExpectedInterest = async (loanId: number) => {
  const response = await fetch(
    `${API_BASE_URL}/loans/${loanId}/expected-repayment-split?paymentDate=${selectedDate}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const data = await response.json();
  setExpectedSplit(data); // Store guidance
};
```

2. **Show two sections - Recommended vs. Your Entry:**
```typescript
// Show in dialog
<div className="grid grid-cols-2 gap-4">
  {/* Left: What system recommends */}
  <Card className="bg-blue-50">
    <h3>Recommended (Reducing Balance)</h3>
    <div>Expected Interest: KES {expectedSplit.expectedInterest}</div>
    <div>Suggested Principal: KES {expectedSplit.suggestedPrincipal}</div>
  </Card>
  
  {/* Right: What treasurer is entering */}
  <Card>
    <h3>Your Entry</h3>
    <input placeholder="Principal" value={principal} 
           onChange={(e) => {
             const p = parseFloat(e.target.value);
             setInterest(total - p);
           }} />
    <div>Calculated Interest: KES {interest}</div>
  </Card>
</div>

{/* Show match status */}
{Math.abs(interest - expectedSplit.expectedInterest) < 1 ? (
  <AlertSuccess>✓ Matches recommended split</AlertSuccess>
) : (
  <AlertWarning>⚠ Different from recommended (Review carefully)</AlertWarning>
)}
```

3. **Allow treasurer to auto-fill from recommendation:**
```typescript
<Button onClick={() => {
  setPrincipal(expectedSplit.suggestedPrincipal);
  setInterest(expectedSplit.expectedInterest);
}}>
  Use Recommended Split
</Button>
```

---

### PHASE 5: Monthly Contributions (Bulk Upload)

**File:** Excel upload for monthly repayments

**Current columns:**
```
Employee ID | Loan# | Repayment Total | [Principal] | [Interest]
```

**What changes:**
- Principal and Interest columns now **mandatory** (not optional)
- Treasurer must supply both OR system calculates interest guidance
- Validation: principal + interest = repayment total

**Processing in `BulkProcessingService`:**
1. For each row, fetch expected interest
2. Compare with what's in the file
3. Log warning if mismatch ("Expected interest was X, but file shows Y")
4. Record what's in the file (treasurer's decision)

---

### PHASE 6: Loan Migration (Historical Data)

**File:** `LoanMigrationService.java`

**What happens during migration:**
```java
// For each migrated loan:
loan.setOutstandingBalance(item.getOutstandingBalance()); 
// ↑ This is what they still owe (principal + accrued interest mixed together)

// Don't recalculate totalInterest, it's historical
loan.setTotalInterest(item.getTotalInterest());

// Going forward, future repayments should split principal vs interest
// based on reducing balance, NOT the pre-calculated total
```

**Key point:**
- Migration brings in `outstanding_balance` from old system
- This amount is treated as starting point
- We DON'T know exactly how much of it is principal vs interest
- As repayments come in, **treasurer must specify the split** for each payment
- System calculates expected interest to guide them

**Suggested UI for migrated loans:**
```
Migrated Loan: L-001 (from old system)
Outstanding Balance: 105,000.00

⚠️ Note: This is a migrated loan. Historical interest calculations 
   may differ from current reducing balance method.
   
For future repayments, interest will be calculated based on 
current outstanding balance and reducing balance method.

Recommended split for today's payment:
├─ Expected Interest: 1,296.99
└─ Suggested Principal: 8,286.34
```

---

## DATABASE CHANGES NEEDED

### 1. Add Last Payment Date Tracking (OPTIONAL, for optimization)

```sql
-- V125__Add_last_payment_date_to_loans.sql

ALTER TABLE loans ADD COLUMN last_payment_date DATETIME NULL;

-- Update existing migrated loans to use disbursement date as baseline
UPDATE loans 
SET last_payment_date = disbursement_date 
WHERE migration_status = 'MIGRATED';

-- Index for performance
CREATE INDEX idx_loans_last_payment_date ON loans(last_payment_date);
```

**Why:** Faster interest calculation (don't need to query repayment history)

### 2. Ensure Loan Repayment Fields Are Set

```sql
-- Verify fields exist (should already be there)
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'loan_repayments' 
AND COLUMN_NAME IN ('principal_amount', 'interest_amount');
```

**These should exist:**
- `loan_repayments.principal_amount` ✓
- `loan_repayments.interest_amount` ✓

---

## FILES TO MODIFY (PRIORITY ORDER)

### MUST DO (Core Functionality)

1. **Create interest calculator service**
   - File: `backend/src/main/java/com/minet/sacco/service/ReducingBalanceInterestCalculator.java`
   - Status: NEW

2. **Add endpoint for interest guidance**
   - File: `backend/src/main/java/com/minet/sacco/controller/LoanController.java`
   - Status: MODIFY - Add new method

3. **Update loan repayment recording UI**
   - File: `minetsacco-main/src/pages/LoanRepaymentRecording.tsx`
   - Status: MODIFY - Enhance dialog, add guidance

4. **Add database migration (last payment date)**
   - File: `backend/src/main/resources/db/migration/V125__Add_last_payment_date_to_loans.sql`
   - Status: NEW

### SHOULD DO (Consistency)

5. **Update bulk processing to handle splits**
   - File: `backend/src/main/java/com/minet/sacco/service/BulkProcessingService.java`
   - Status: MODIFY - Add interest guidance logging

6. **Update loan migration to note reducing balance**
   - File: `backend/src/main/java/com/minet\sacco/service/LoanMigrationService.java`
   - Status: MODIFY - Add comments/logging

### NICE TO HAVE (Reporting)

7. **Interest summary report**
   - File: NEW ReportService method
   - Shows total interest collected vs. expected

---

## HOW IT WORKS END-TO-END

### Scenario: Treasurer Records Repayment

```
1. Treasurer opens Loan Repayment Recording
   └─ Selects Loan L-001

2. System fetches:
   ├─ Outstanding Balance: 105,000
   ├─ Last Payment Date: 2026-05-19
   └─ Calls: GET /loans/1/expected-repayment-split?paymentDate=2026-06-18

3. System calculates expected interest:
   ├─ Days elapsed: 30
   ├─ Interest formula: 105,000 × (15% / 365) × 30
   └─ Expected Interest: 1,296.99

4. UI displays:
   ┌─ Recommended Split:
   │  ├─ Interest: 1,296.99
   │  └─ Principal: 8,286.34
   │
   └─ Your Entry fields (for manual override if needed)

5. Treasurer can:
   ├─ Click [Use Recommended] → Auto-fills principal & interest
   ├─ Or manually enter principal → Interest auto-calculates
   ├─ Or manually enter interest → Principal auto-calculates
   └─ Total amount validates: principal + interest = total ✓

6. Treasurer clicks [Record Repayment]

7. System records:
   ├─ LoanRepayment.principal_amount = 8,286.34
   ├─ LoanRepayment.interest_amount = 1,296.99
   ├─ Updates Loan.outstanding_balance = 96,716.66
   ├─ Updates Loan.interest_remaining = (if tracked)
   ├─ Creates Transaction records:
   │  ├─ LOAN_REPAYMENT: 9,583.33
   │  └─ INTEREST: 1,296.99 (for GL posting)
   └─ Audit logged

8. Next month's interest calculation:
   └─ Based on new outstanding: 96,716.66
```

### For Bulk Monthly Upload

```
1. Excel file with columns:
   Employee ID | Loan # | Total | Principal | Interest

2. System processes each row:
   ├─ Fetches expected interest for that date
   ├─ Compares with file data
   ├─ If mismatch: Logs warning but records what's in file
   ├─ Validates: principal + interest = total
   └─ Records repayment with treasurer's split

3. Result: Each payment properly split and tracked
```

### For Migrated Loans

```
1. Loan imported with outstanding_balance from old system
   (e.g., 105,000 - mix of principal and accrued interest)

2. When first repayment comes in:
   ├─ System suggests split based on current outstanding
   ├─ Example: 1,296.99 interest on 105,000 for 30 days
   └─ Treasurer enters split (or uses suggestion)

3. Going forward:
   ├─ Each repayment reduces outstanding balance
   ├─ Next interest calculation based on new balance
   └─ Historical interest calculation method doesn't matter
```

---

## VALIDATION & SAFETY

### What Gets Validated

```
✓ principal + interest = total amount
✓ principal >= 0
✓ interest >= 0
✓ amount > 0
✓ amount <= outstanding_balance
✓ loan status is DISBURSED
✓ member exists
✓ treasurer has permission
```

### What Gets Logged

```
✓ Actual split recorded
✓ Expected split (for comparison)
✓ Who recorded it
✓ When it was recorded
✓ If split differs from expected (warning level)
```

### What Treasury Can Override

```
✓ Principal amount (if they disagree with suggestion)
✓ Interest amount (if they disagree with suggestion)
✓ But MUST maintain: principal + interest = total
```

---

## SUCCESS CRITERIA

When complete, we should be able to answer:

1. ✅ How much of each payment went to interest? 
   → `LoanRepayment.interestAmount`

2. ✅ How much total interest has the SACCO earned from this loan?
   → `SUM(LoanRepayment.interestAmount) WHERE loan_id = ?`

3. ✅ Is the interest calculation following reducing balance method?
   → Expected interest should decrease each month

4. ✅ Can treasurer see guidance before entering?
   → UI shows recommended split

5. ✅ Can treasurer override if needed?
   → Manual fields allow override

6. ✅ Are GL accounts posting interest correctly?
   → `LoanRepayment.interestAmount` goes to interest income account

7. ✅ Does this work for migrated loans too?
   → Interest calculated going forward from outstanding balance

---

## TESTING CHECKLIST

- [ ] Calculator returns correct interest for 30 days
- [ ] Calculator returns correct interest for other durations
- [ ] Endpoint returns guidance correctly
- [ ] UI shows recommendation
- [ ] Manual entry overrides work
- [ ] Principal + interest = total validation works
- [ ] Repayment recorded correctly
- [ ] Outstanding balance updates correctly
- [ ] Next month's interest calculation is lower (reducing balance)
- [ ] Bulk upload validates splits
- [ ] Migrated loans work correctly
- [ ] GL accounts post interest correctly
- [ ] Audit trail shows split details
- [ ] Reports show interest vs principal breakdown

---

## SUMMARY: WHAT GETS DONE

| Component | What Happens |
|-----------|--------------|
| **Calculator** | Calculates monthly interest based on outstanding balance and days |
| **API Endpoint** | Provides guidance (expected interest, suggested principal) |
| **UI Form** | Shows recommended split, allows treasurer to enter actual split |
| **Backend Recording** | Accepts principal/interest split, validates, records both |
| **Database** | Stores principal_amount and interest_amount separately |
| **GL Posting** | Uses interest_amount for income account |
| **Migration** | Uses outstanding_balance as starting point, calculates interest going forward |
| **Bulk Upload** | Validates principal + interest = total for each row |
| **Reporting** | Can report total interest earned, principal collected, etc. |

---

## RESULT

✅ **SACCO knows exactly how much profit (interest) came from each loan payment**
✅ **Reducing balance method properly calculated month-by-month**
✅ **Treasurer has guidance but maintains control**
✅ **GL accounts post interest revenue correctly**
✅ **Audit trail shows split for each repayment**
