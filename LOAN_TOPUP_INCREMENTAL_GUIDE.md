# Loan Top-Up Feature - Incremental Model

## Correct Design (Per Company Request)

The company wants **incremental top-ups** on the SAME loan, NOT creating a new loan. Previous payments are credited, and the member just adds more funds to their existing loan.

---

## How It Works

### Example Scenario

**Original Loan (Loan #366):**
- Principal: KES 1,000,000
- Term: 48 months
- Already Paid: KES 300,000
- Outstanding: KES 700,000

**Member Wants Top-Up:**
- Additional Amount: KES 50,000

**After Top-Up (SAME Loan #366):**
- Original Principal: KES 1,000,000
- Total Top-Up Added: KES 50,000
- New Outstanding: KES 750,000
- Previous Payments: KES 300,000 (credited/recognized)
- New Guarantors: Added to SAME loan
- Remaining Term: 48 months (same as before)

---

## The Process (Step by Step)

### Step 1: User Clicks "Top-Up This Loan"

From loan details page, above "Repayment Progress" section

### Step 2: Top-Up Dialog Shows

```
┌────────────────────────────────────────────────────┐
│ ✨ Top-Up Loan #366                                │
│                                                     │
│ Current Loan Status:                                │
│ • Original Principal: KES 1,000,000                │
│ • Already Paid: KES 300,000                        │
│ • Current Outstanding: KES 700,000                 │
│                                                     │
│ Add Top-Up Amount: [_____50,000_____] KES         │
│                                                     │
│ ┌─────────────────────────────────────────┐        │
│ │ 📊 AFTER TOP-UP SUMMARY                 │        │
│ │                                          │        │
│ │ Your Payments (Credited): KES 300,000   │        │
│ │ Current Outstanding:      KES 700,000   │        │
│ │ Top-Up Amount:            KES  50,000   │        │
│ │ ────────────────────────────────────────│        │
│ │ NEW OUTSTANDING:          KES 750,000   │        │
│ │                                          │        │
│ │ New Interest (12%):       KES 360,000   │        │
│ │ New Total Repayable:      KES 1,110,000 │        │
│ │ New Monthly Payment:      KES 23,125    │        │
│ │ Term Remaining:           48 months     │        │
│ └─────────────────────────────────────────┘        │
│                                                     │
│ Add Guarantors for Top-Up:                         │
│ [+ Add Guarantor]                                  │
│                                                     │
│ Purpose: [________________________________]        │
│                                                     │
│ ℹ️ Your previous guarantors remain. New guarantors│
│   will be added for the additional KES 50,000.    │
│                                                     │
│ [Cancel]                   [Submit Top-Up Request] │
└────────────────────────────────────────────────────┘
```

### Step 3: System Processing

When top-up is submitted:

1. **Validate Current Loan:**
   - Status must be DISBURSED
   - Outstanding balance > 0
   - Member eligible for increased amount

2. **Update SAME Loan:**
   ```
   outstanding_balance = 700,000 + 50,000 = 750,000
   total_topup_amount = total_topup_amount + 50,000
   topup_count = topup_count + 1
   last_topup_date = NOW()
   principal_before_topup = 700,000 (for history)
   ```

3. **Recalculate Loan Terms:**
   - New Interest: 750,000 × 12% × 4 years = 360,000
   - New Total Repayable: 750,000 + 360,000 = 1,110,000
   - New Monthly Payment: 1,110,000 ÷ 48 = 23,125

4. **Add New Guarantors:**
   - Old guarantors remain (for original amount)
   - New guarantors added (for top-up amount)
   - All go through approval workflow

5. **Record in History Table:**
   ```sql
   INSERT INTO loan_topup_history (
     loan_id, topup_amount, outstanding_before_topup,
     outstanding_after_topup, principal_paid_before_topup, topup_date
   ) VALUES (
     366, 50000, 700000, 750000, 300000, NOW()
   );
   ```

6. **Disburse Top-Up Amount:**
   - Credit member account: KES 50,000
   - Update loan outstanding: KES 750,000
   - Create accounting entries

---

## Database Schema

### loans Table (Updated Fields)

```sql
-- Track cumulative top-ups
total_topup_amount DECIMAL(15,2) DEFAULT 0
topup_count INT DEFAULT 0
last_topup_date TIMESTAMP NULL
principal_before_topup DECIMAL(15,2) NULL
```

### New Table: loan_topup_history

```sql
CREATE TABLE loan_topup_history (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    topup_amount DECIMAL(15,2) NOT NULL,
    outstanding_before_topup DECIMAL(15,2) NOT NULL,
    outstanding_after_topup DECIMAL(15,2) NOT NULL,
    principal_paid_before_topup DECIMAL(15,2) NOT NULL,
    new_guarantors_added INT DEFAULT 0,
    topup_date TIMESTAMP NOT NULL,
    processed_by BIGINT,
    notes TEXT
);
```

**Example Data After Top-Up:**

```
Loan #366:
  amount: 1,000,000
  outstanding_balance: 750,000
  total_topup_amount: 50,000
  topup_count: 1
  last_topup_date: 2026-07-28 15:00:00

loan_topup_history #1:
  loan_id: 366
  topup_amount: 50,000
  outstanding_before_topup: 700,000
  outstanding_after_topup: 750,000
  principal_paid_before_topup: 300,000
  topup_date: 2026-07-28 15:00:00
```

---

## UI Display on Loan Details Page

### Show Top-Up History Above "Repayment Progress"

```
┌────────────────────────────────────────────────────┐
│ Loan Details                                        │
│                                                     │
│ ID: 366                    Status: DISBURSED       │
│ Member: Mr Katee Mutunga                           │
│ Product: Normal Loan                               │
│                                                     │
│ ┌──────────────────────────────────────────────┐  │
│ │ 🔄 TOP-UP HISTORY (1 Top-Up)                 │  │
│ │                                               │  │
│ │ Original Principal:      KES 1,000,000       │  │
│ │ Total Top-Ups Added:     KES 50,000          │  │
│ │ Last Top-Up:             Jul 28, 2026        │  │
│ │                                               │  │
│ │ [View Full Top-Up History] [Add Another Top-Up]│
│ └──────────────────────────────────────────────┘  │
│                                                     │
│ Amount Details                                      │
│ Principal: KES 1,000,000                           │
│ Rate: 12%                                          │
│ Term: 48 months                                    │
│ Interest: KES 360,000                              │
│ Outstanding: KES 750,000                           │
│                                                     │
│ Repayment Progress                                 │
│ Repayment Status: 30.00%                           │
│                                                     │
│ Original Principal:     KES 1,000,000              │
│ Top-Ups Added:          KES    50,000              │
│ Total Principal:        KES 1,050,000              │
│ Principal Repaid:       KES   300,000              │
│ Principal Outstanding:  KES   750,000              │
│ Total Repaid:           KES   315,000 (with int.)  │
│ Outstanding Balance:    KES   750,000              │
│                                                     │
│ ...                                                 │
└────────────────────────────────────────────────────┘
```

### Top-Up History Detail View

```
┌────────────────────────────────────────────────────┐
│ Top-Up History for Loan #366                       │
│                                                     │
│ #1 - July 28, 2026                                 │
│ ├─ Top-Up Amount: KES 50,000                       │
│ ├─ Outstanding Before: KES 700,000                 │
│ ├─ Outstanding After: KES 750,000                  │
│ ├─ Already Paid: KES 300,000 (credited)            │
│ ├─ New Guarantors Added: 2                         │
│ └─ Processed By: John Treasurer                    │
│                                                     │
│ [Close]                                             │
└────────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Add Top-Up to Existing Loan

**POST** `/api/loans/{loanId}/add-topup`

**Request:**
```json
{
  "topupAmount": 50000.00,
  "purpose": "Additional business capital",
  "newGuarantors": [
    {
      "guarantorMemberNumber": "EMP789",
      "guaranteeAmount": 30000.00
    },
    {
      "guarantorMemberNumber": "EMP101",
      "guaranteeAmount": 20000.00
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Top-up of KES 50,000 added to loan successfully",
  "data": {
    "loanId": 366,
    "topupAmount": 50000.00,
    "outstandingBefore": 700000.00,
    "outstandingAfter": 750000.00,
    "principalAlreadyPaid": 300000.00,
    "totalTopups": 50000.00,
    "topupCount": 1,
    "newMonthlyPayment": 23125.00,
    "newTotalRepayable": 1110000.00,
    "topupDate": "2026-07-28T15:00:00"
  }
}
```

### 2. Get Top-Up Preview

**GET** `/api/loans/{loanId}/topup-preview?amount=50000`

**Response:**
```json
{
  "currentOutstanding": 700000.00,
  "principalAlreadyPaid": 300000.00,
  "topupAmount": 50000.00,
  "newOutstanding": 750000.00,
  "currentInterest": 336000.00,
  "newInterest": 360000.00,
  "currentMonthlyPayment": 22000.00,
  "newMonthlyPayment": 23125.00,
  "currentTotalRepayable": 1056000.00,
  "newTotalRepayable": 1110000.00,
  "eligibilityCheck": {
    "eligible": true,
    "maxTopupAllowed": 250000.00
  }
}
```

### 3. Get Top-Up History

**GET** `/api/loans/{loanId}/topup-history`

**Response:**
```json
{
  "loanId": 366,
  "topupCount": 1,
  "totalTopupAmount": 50000.00,
  "topups": [
    {
      "id": 1,
      "topupAmount": 50000.00,
      "outstandingBefore": 700000.00,
      "outstandingAfter": 750000.00,
      "principalPaidBefore": 300000.00,
      "newGuarantorsAdded": 2,
      "topupDate": "2026-07-28T15:00:00",
      "processedBy": "John Treasurer"
    }
  ]
}
```

---

## Accounting Entries

### When Top-Up is Approved and Disbursed:

```
1. Record top-up receivable:
   DR: Loan #366 Account (Asset)         50,000.00
   CR: Loans Receivable                  50,000.00

2. Disburse cash to member:
   DR: Member Savings/Current Account    50,000.00
   CR: Cash/Bank                         50,000.00

3. Update loan ledger:
   DR: Loan Outstanding Balance          50,000.00
   CR: Loan Principal Account             50,000.00
```

**Net Effect:**
- Loan outstanding increases by KES 50,000
- Member receives KES 50,000 in cash
- Previous payments remain credited
- System 100% accurate

---

## Business Rules

### 1. **Eligibility for Top-Up**
- ✅ Loan status must be DISBURSED
- ✅ Outstanding balance > 0
- ✅ Member must be eligible for increased total amount
- ✅ No active pending loan applications
- ✅ No suspensions or exits

### 2. **Top-Up Limits**
- Minimum top-up: KES 10,000
- Maximum top-up: Based on member eligibility
- Cannot exceed maximum loan amount for product

### 3. **Guarantors**
- Old guarantors remain for original amount
- New guarantors added for top-up amount
- New guarantors must go through approval
- Total guarantee must cover new outstanding

### 4. **Interest Recalculation**
- Interest calculated on NEW outstanding balance
- Same interest rate as original loan
- Same term as original loan
- Previous interest payments credited

### 5. **Repayment Schedule**
- Monthly payment recalculated
- Based on new outstanding + new interest
- Divided by remaining months in term
- Previous payments reduce principal

---

## Benefits

### For Members:
- ✅ **Keep same loan** - Don't restart approval process
- ✅ **Credit for payments** - Past payments recognized
- ✅ **Clear history** - See all top-ups in one place
- ✅ **Easy to understand** - Simple addition of funds

### For SACCO:
- ✅ **100% accurate** - Proper accounting maintained
- ✅ **Complete audit trail** - Every top-up tracked
- ✅ **Single loan management** - No multiple loans
- ✅ **Flexible guarantors** - Can add incrementally

---

## Implementation Summary

### What I've Done:
1. ✅ Updated database schema (V144 migration)
2. ✅ Modified Loan entity with top-up fields
3. ✅ Created loan_topup_history table
4. ✅ Designed complete documentation

### What's Next:
1. Create DTO classes (LoanTopUpRequest, LoanTopUpResponse)
2. Implement service logic in LoanService
3. Add controller endpoints
4. Create frontend UI components
5. Test complete flow

---

**Last Updated:** 2026-07-28  
**Version:** 2.0 (Corrected to Incremental Model)
