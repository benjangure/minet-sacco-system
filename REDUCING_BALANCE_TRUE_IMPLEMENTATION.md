# True Reducing Balance Implementation: Complete Journey

## Overview

This document describes the complete journey of a loan from disbursement through monthly repayments using the **true reducing balance method**, where interest is calculated monthly based on the remaining principal, not pre-calculated upfront.

---

## PHASE 1: LOAN DISBURSEMENT

### What Happens Now (Wrong)
When a loan is disbursed:
```java
loan.amount = 100,000
loan.interestRate = 15% (annual)
loan.termMonths = 12

// System calculates UPFRONT using simple interest:
loan.totalInterest = 100,000 × 15% × (12/12) = 15,000 (FIXED)
loan.totalRepayable = 115,000
loan.monthlyRepayment = 115,000 / 12 = 9,583.33
loan.outstandingBalance = 115,000
```

**Problem:** Total interest is predetermined. Treasurer can't know the actual interest for each month because it changes based on remaining balance.

### What Should Happen (True Reducing Balance)
When a loan is disbursed:
```java
loan.amount = 100,000
loan.interestRate = 15% (annual)
loan.termMonths = 12

// DON'T pre-calculate totalInterest
// DON'T pre-calculate monthlyRepayment

// Instead, store:
loan.outstandingBalance = 100,000  // Will reduce with each payment
loan.nextInterestCalculationDate = disbursementDate + 1 month
```

**Key Difference:** Interest will be calculated month-by-month based on remaining balance, not upfront.

---

## PHASE 2: FIRST MONTH - RECORDING REPAYMENT

### Scenario
Loan disbursed: June 18, 2026  
Member pays: July 18, 2026 (exactly 1 month later)  
Payment: 9,583.33 KES

### True Reducing Balance Calculation

**Step 1: Calculate This Month's Interest**
```
Outstanding Balance at payment time: 100,000
Days elapsed: 30 days (June 18 - July 18)
Annual Interest Rate: 15%

Monthly Interest = Outstanding × (Annual Rate / 12)
                 = 100,000 × (15% / 12)
                 = 100,000 × 1.25%
                 = 1,250 KES

This is the FIRST month's interest (not pre-determined)
```

**Step 2: System Shows Guidance to Treasurer**

When treasurer opens repayment dialog:
```
Loan: L-001
Outstanding Balance: 100,000
Current Month's Interest (Reducing Balance): 1,250 KES

Suggested Split:
├─ Interest: 1,250 KES (mandatory for this month)
└─ Principal: 8,333.33 KES (9,583.33 total - 1,250 interest)

Treasurer's Entry:
├─ Total Amount: 9,583.33
├─ Principal: [8,333.33] (entered or accepted)
└─ Interest: [1,250] (auto-calculated as total - principal)
```

**Step 3: Record Repayment**

System records:
```
LoanRepayment:
├─ principal_amount: 8,333.33
├─ interest_amount: 1,250.00
├─ total_amount: 9,583.33
└─ payment_date: 2026-07-18

Loan Update:
├─ outstandingBalance: 100,000 - 9,583.33 = 90,416.67  ← REDUCED
├─ interestRemaining: (if tracked) = total_interest_ever_owed - 1,250
└─ status: DISBURSED (still paying)

GL Posting:
├─ Debit Cash Account: 9,583.33
├─ Credit Interest Income: 1,250.00 ← SACCO's profit
└─ Credit Principal/Loan Account: 8,333.33
```

---

## PHASE 3: SECOND MONTH - INTEREST DECREASES

### Scenario
Member pays: August 18, 2026 (another month later)  
Payment: 9,583.33 KES (same monthly amount)

### True Reducing Balance Calculation

**Step 1: Calculate This Month's Interest**
```
Outstanding Balance at payment time: 90,416.67  ← DECREASED
Days elapsed: 30 days
Annual Interest Rate: 15%

Monthly Interest = Outstanding × (Annual Rate / 12)
                 = 90,416.67 × 1.25%
                 = 1,130.21 KES  ← LOWER than month 1!

This is why it's called REDUCING BALANCE
```

**Step 2: Guidance to Treasurer**

```
Loan: L-001
Outstanding Balance: 90,416.67  ← Changed from last month
Current Month's Interest (Reducing Balance): 1,130.21 KES

Suggested Split:
├─ Interest: 1,130.21 KES ← Decreased from 1,250
└─ Principal: 8,453.12 KES ← Increased (can pay more principal)

Treasurer's Entry:
├─ Total Amount: 9,583.33
├─ Principal: [8,453.12]
└─ Interest: [1,130.21]
```

**Step 3: Record Repayment**

System records:
```
LoanRepayment:
├─ principal_amount: 8,453.12
├─ interest_amount: 1,130.21
├─ total_amount: 9,583.33
└─ payment_date: 2026-08-18

Loan Update:
├─ outstandingBalance: 90,416.67 - 9,583.33 = 80,833.34  ← Further reduced
└─ status: DISBURSED

GL Posting:
├─ Interest Income: 1,130.21 ← Less profit this month
└─ Principal: 8,453.12 ← More toward principal reduction
```

---

## PHASE 4: THIRD MONTH & BEYOND

### Pattern Continues

**Month 3:**
```
Outstanding: 80,833.34
Interest: 80,833.34 × 1.25% = 1,010.42 KES  ← Even lower
Principal: 9,583.33 - 1,010.42 = 8,572.91 KES
```

**Month 4:**
```
Outstanding: 71,260.43
Interest: 71,260.43 × 1.25% = 890.76 KES  ← Continues decreasing
Principal: 9,583.33 - 890.76 = 8,692.57 KES
```

### Why Interest Keeps Decreasing
- Same total payment (9,583.33) each month
- Interest is calculated on REMAINING balance
- As principal is paid, balance gets smaller
- Smaller balance = smaller interest charge
- More of each payment goes to principal over time

---

## PHASE 5: LOAN FULLY REPAID

After 12 months:
```
Total Paid: 9,583.33 × 12 = 115,000 KES
Total Principal Paid: 100,000 KES
Total Interest Paid: SUM(all monthly interest) ≈ 8,000-9,000 KES

Why Less Than Pre-Calculated 15,000?
Because interest was calculated on REDUCING balance, not fixed amount.
```

---

## EXAMPLE: Full 12-Month Journey

| Month | Outstanding Start | Monthly Interest | Principal | Total Payment | Outstanding End |
|-------|-------------------|-----------------|-----------|---------------|-----------------|
| 1 | 100,000.00 | 1,250.00 | 8,333.33 | 9,583.33 | 91,666.67 |
| 2 | 91,666.67 | 1,145.83 | 8,437.50 | 9,583.33 | 83,229.17 |
| 3 | 83,229.17 | 1,040.36 | 8,542.97 | 9,583.33 | 74,686.20 |
| 4 | 74,686.20 | 933.58 | 8,649.75 | 9,583.33 | 66,036.45 |
| 5 | 66,036.45 | 825.46 | 8,757.87 | 9,583.33 | 57,278.58 |
| 6 | 57,278.58 | 715.98 | 8,867.35 | 9,583.33 | 48,411.23 |
| 7 | 48,411.23 | 605.14 | 8,978.19 | 9,583.33 | 39,433.04 |
| 8 | 39,433.04 | 492.91 | 9,090.42 | 9,583.33 | 30,342.62 |
| 9 | 30,342.62 | 379.28 | 9,204.05 | 9,583.33 | 21,138.57 |
| 10 | 21,138.57 | 264.23 | 9,319.10 | 9,583.33 | 11,819.47 |
| 11 | 11,819.47 | 147.74 | 9,435.59 | 9,583.33 | 2,383.88 |
| 12 | 2,383.88 | 29.80 | 9,553.53 | 9,583.33 | 0.00 |
| **TOTAL** | | **8,180.31** | **100,000** | **115,000** | |

**Notice:**
- Interest starts at 1,250 and ends at 29.80
- Total interest paid: 8,180.31 (NOT the pre-calculated 15,000)
- SACCO profit is actual interest collected, not what was pre-calculated

---

## IMPLEMENTATION: SYSTEM COMPONENTS NEEDED

### 1. Interest Calculator Service (Backend)
```java
@Service
public class ReducingBalanceInterestCalculator {
    
    public BigDecimal calculateMonthlyInterest(Loan loan) {
        BigDecimal monthlyRate = loan.getInterestRate()
            .divide(new BigDecimal("1200"), 4, HALF_UP);  // Annual % to monthly
        
        BigDecimal monthlyInterest = loan.getOutstandingBalance()
            .multiply(monthlyRate)
            .setScale(2, HALF_UP);
        
        return monthlyInterest;  // Returns 1,250 for month 1, 1,145.83 for month 2, etc.
    }
}
```

### 2. API Endpoint (Backend)
```java
@GetMapping("/{loanId}/calculate-monthly-interest")
public ResponseEntity<?> getMonthlyInterest(@PathVariable Long loanId) {
    Loan loan = loanService.getLoanById(loanId);
    BigDecimal interest = calculator.calculateMonthlyInterest(loan);
    BigDecimal suggestedPrincipal = loan.getMonthlyRepayment().subtract(interest);
    
    return ResponseEntity.ok(new {
        currentOutstandingBalance: loan.getOutstandingBalance(),
        calculatedMonthlyInterest: interest,
        suggestedPrincipal: suggestedPrincipal,
        totalPayment: loan.getMonthlyRepayment()
    });
}
```

### 3. Enhanced Repayment Dialog (Frontend)
```
Outstanding Balance: 100,000

System Calculation (Reducing Balance):
├─ Monthly Interest: 1,250
└─ Suggested Principal: 8,333.33

You Enter:
├─ Total Amount: [9,583.33]
├─ Principal: [8,333.33] or click "Use Suggested"
└─ Interest: [1,250] (auto-calculated)

[Validates: principal + interest = total]
```

### 4. Recording Repayment (Backend)
```java
public LoanRepayment recordRepayment(Long loanId, BigDecimal totalAmount, 
                                      BigDecimal principalAmount) {
    Loan loan = getLoan(loanId);
    BigDecimal interestAmount = totalAmount.subtract(principalAmount);
    
    // Validate
    if (principalAmount.add(interestAmount).compareTo(totalAmount) != 0) {
        throw new Exception("Principal + interest must equal total");
    }
    
    // Record exactly what treasurer entered
    LoanRepayment repayment = new LoanRepayment();
    repayment.setPrincipalAmount(principalAmount);
    repayment.setInterestAmount(interestAmount);
    repayment.setAmount(totalAmount);
    loanRepaymentRepository.save(repayment);
    
    // Update loan
    loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(totalAmount));
    loanRepository.save(loan);
    
    // Post to GL
    // Interest (1,250) → GL Income Account
    // Principal (8,333.33) → GL Loan/Principal Account
    
    return repayment;
}
```

### 5. Excel Upload Processing (Backend)
```
Monthly Contributions Excel File:
┌──────────┬────────┬─────────────┬──────────┬──────────┐
│ EmpID    │ Loan#  │ Total Paid  │ Principal│ Interest │
├──────────┼────────┼─────────────┼──────────┼──────────┤
│ EMP001   │ L-001  │ 9,583.33   │ 8,333.33 │ 1,250.00 │
│ EMP002   │ L-002  │ 5,000.00   │ 4,500.00 │ 500.00   │
└──────────┴────────┴─────────────┴──────────┴──────────┘

System Validates:
- Principal + Interest = Total ✓
- Records exactly what's in file
- Calculates expected interest for comparison (optional warning)
```

---

## LOAN MIGRATION HANDLING

### For Old Migrated Loans
```
Old System Data:
├─ outstanding_balance: 105,000 (mixed principal + accrued interest)
├─ total_interest_calculated: 15,000
└─ created_date: 2025-01-01

New System:
├─ Keep outstanding_balance: 105,000 (this is what's owed)
├─ Don't recalculate totalInterest (historical)
├─ Going forward, calculate interest monthly on current balance
└─ First repayment will use reducing balance method
```

**Example Migration:**
```
Migrated Loan L-001
Outstanding: 105,000
First monthly interest (reducing balance): 105,000 × 1.25% = 1,312.50

This might differ from old system's calculation, but it's correct
for reducing balance going forward.
```

---

## DATABASE FIELDS NEEDED

### Loans Table (Already Exists)
```sql
outstandingBalance          -- Current amount owed
interestRate                -- Annual % (15%)
```

### Loan Repayments Table (Already Exists)
```sql
principal_amount            -- How much went to principal
interest_amount             -- How much was interest (SACCO's profit)
amount                      -- Total paid
payment_date                -- When paid
```

### Optional Enhancement
```sql
-- Add to loans table for faster calculation
last_payment_date           -- When last payment was recorded
last_calculated_interest    -- Interest calculated for last payment
```

---

## VALIDATION RULES

At repayment recording:
```
✓ principal + interest = total amount paid
✓ principal ≥ 0
✓ interest ≥ 0
✓ total > 0
✓ total ≤ outstanding_balance
✓ loan status = DISBURSED
✓ principal and interest both provided
```

Optional Warnings:
```
⚠ Expected interest was X but you entered Y (difference alert)
⚠ That's a significant difference, verify amounts
```

---

## AUDIT TRAIL

Every repayment records:
```
Payment: 9,583.33
├─ Principal: 8,333.33 (reduces outstanding balance)
├─ Interest: 1,250.00 (SACCO's income, goes to GL)
├─ By: Treasurer John
├─ Date: 2026-07-18
└─ Note: "Reducing balance, month 1. Next interest will be lower."
```

---

## REPORTING

You can now answer:
```
Q: How much interest did we earn from Loan L-001?
A: SUM(LoanRepayment.interest_amount WHERE loan_id = L-001)
   = 8,180.31 KES

Q: How much did the member pay toward principal?
A: SUM(LoanRepayment.principal_amount WHERE loan_id = L-001)
   = 100,000.00 KES

Q: Is our calculation correct?
A: principal + interest = 100,000 + 8,180.31 = 108,180.31 KES
   (Not 115,000 because reducing balance is less than simple interest)
```

---

## FILES TO CREATE/MODIFY

### Create (New)
1. `ReducingBalanceInterestCalculator.java` - Interest calculation service

### Modify (Backend)
1. `LoanController.java` - Add interest calculation endpoint
2. `LoanRepaymentService.recordRepayment()` - Keep as-is, it already supports principal/interest split
3. `BulkProcessingService.java` - Validate principal + interest = total in Excel uploads

### Modify (Frontend)
1. `LoanRepaymentRecording.tsx` - Add interest guidance display and manual entry

### Database
1. Create migration for `last_payment_date` column (optional)

---

## KEY INSIGHT

**The system is 90% ready:**
- Database columns for principal/interest already exist ✓
- Backend validation already checks principal + interest = total ✓
- GL posting for interest income already happens ✓
- Only missing: Monthly interest calculation and UI guidance ✓

**What changes:**
1. Calculate interest monthly (not upfront)
2. Show treasurer what it should be
3. Record what they actually enter
4. Track it for GL posting

That's it. True reducing balance tracking.

---

## SUCCESS METRIC

When done:
```
✓ Can answer "How much profit (interest) came from this loan?"
✓ Interest amount decreases each month (because balance reduces)
✓ GL accounts post correct interest to income account
✓ Audit trail shows principal vs interest for each payment
✓ Reports show actual SACCO profit, not pre-calculated guesses
```
