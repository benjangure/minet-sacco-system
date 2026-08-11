# Loan Top-Up Feature - Complete Guide

## Overview

The Loan Top-Up feature allows members to **consolidate their existing loan with additional funds** in a single new loan. This maintains 100% accounting accuracy while providing a clear, user-friendly experience.

---

## How It Works

### Example Scenario

**Current Loan (ID: 366):**
- Principal: KES 329,297
- Outstanding Balance: KES 138,635.69
- Already Repaid: KES 190,661.31
- Status: DISBURSED

**Member Wants to Top-Up:**
- Additional Amount Needed: KES 200,000

**New Top-Up Loan Created:**
- Total Principal: KES 338,635.69 (Outstanding KES 138,635.69 + New KES 200,000)
- Top-Up Amount: KES 200,000 (tracked separately)
- Interest Rate: 12% (or as configured)
- Term: 48 months (configurable)
- **Can add NEW guarantors** for the top-up amount

---

## The Process

### Step 1: User Initiates Top-Up

From the loan details page, user clicks **"Top-Up This Loan"** button.

### Step 2: Top-Up Application Form

User provides:
1. **Additional Amount**: How much new money they need (e.g., KES 200,000)
2. **Loan Product**: Select product (Normal Loan, Emergency Loan, etc.)
3. **Purpose**: Why they need the top-up
4. **New Guarantors**: Add guarantors for the new combined loan amount

**Auto-Calculated Fields:**
- Outstanding Balance from Parent Loan: KES 138,635.69
- Total New Loan Amount: KES 338,635.69
- Interest on New Loan: Calculated based on total amount
- Monthly Repayment: Based on new loan terms

### Step 3: System Processing (Backend)

When top-up is submitted:

1. **Validate** the parent loan:
   - Must be in DISBURSED status
   - Must have outstanding balance > 0
   - Member must be eligible for new loan amount

2. **Create NEW loan**:
   ```
   New Loan Amount = Parent Outstanding Balance + Top-Up Amount
   Example: KES 138,635.69 + KES 200,000 = KES 338,635.69
   ```

3. **Link loans**:
   - New loan's `parent_loan_id` = Old loan ID
   - New loan's `is_topup` = true
   - New loan's `topup_amount` = KES 200,000

4. **Update parent loan**:
   - Status → REPAID (via top-up)
   - Add audit entry: "Loan settled via top-up to Loan #XYZ"

5. **Process guarantors**:
   - Old guarantors → Released from parent loan
   - New guarantors → Added to new loan
   - Approval workflow starts fresh

6. **Accounting entries**:
   - When new loan is DISBURSED:
     - Debit: Loans Receivable (KES 338,635.69)
     - Credit: Member Loan Account (KES 138,635.69) - Settles old loan
     - Credit: Cash/Bank (KES 200,000) - New money to member

7. **Audit trail**:
   - Parent Loan: "Settled via top-up to Loan #NEW_ID"
   - New Loan: "Top-up of Loan #OLD_ID (Outstanding: KES 138,635.69, New: KES 200,000)"

---

## User Interface

### Loan Details Page - Add Top-Up Button

```
┌─────────────────────────────────────────────────────┐
│ Loan Details                                         │
│                                                      │
│ ID: 366               Status: DISBURSED             │
│ Member: Mr Katee Mutunga                            │
│                                                      │
│ Principal: KES 329,297                              │
│ Outstanding: KES 138,635.69                         │
│ Already Repaid: KES 190,661.31                      │
│                                                      │
│ [Edit Loan Fields]  [Top-Up This Loan] 🔄          │
│                                                      │
│ Repayment Progress                                  │
│ 57.90% Complete                                     │
│ ...                                                  │
└─────────────────────────────────────────────────────┘
```

### Top-Up Application Dialog

```
┌──────────────────────────────────────────────────────────┐
│ ✨ Top-Up Loan #366                                      │
│                                                           │
│ Current Loan Summary:                                    │
│ • Outstanding Balance: KES 138,635.69                   │
│ • Already Repaid: KES 190,661.31                        │
│                                                           │
│ New Top-Up Details:                                      │
│                                                           │
│ Additional Amount Needed: [____________] KES             │
│ Example: 200000                                          │
│                                                           │
│ Loan Product: [Normal Loan ▼]                           │
│                                                           │
│ New Loan Term: [48] months                              │
│                                                           │
│ Purpose: [________________________________]             │
│                                                           │
│ ┌──────────────────────────────────────────┐            │
│ │ 📊 NEW LOAN SUMMARY                      │            │
│ │                                           │            │
│ │ Settlement of Old Loan:  KES 138,635.69  │            │
│ │ New Money to You:        KES 200,000.00  │            │
│ │ ─────────────────────────────────────────│            │
│ │ TOTAL NEW LOAN:          KES 338,635.69  │            │
│ │                                           │            │
│ │ Interest (12%):          KES 162,544.93  │            │
│ │ Total Repayable:         KES 501,180.62  │            │
│ │ Monthly Payment:         KES 10,441.26   │            │
│ └──────────────────────────────────────────┘            │
│                                                           │
│ Add Guarantors:                                          │
│ [+ Add Guarantor]                                        │
│                                                           │
│ ℹ️ Your old guarantors will be released. You need to    │
│   add guarantors for the new loan amount.               │
│                                                           │
│ [Cancel]                      [Submit Top-Up Application]│
└──────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Create Loan Top-Up

**POST** `/api/loans/{loanId}/topup`

**Request Body:**
```json
{
  "topupAmount": 200000.00,
  "loanProductId": 1,
  "termMonths": 48,
  "purpose": "Business expansion and settle existing loan",
  "guarantors": [
    {
      "guarantorMemberNumber": "EMP123",
      "guaranteeAmount": 150000.00
    },
    {
      "guarantorMemberNumber": "EMP456",
      "guaranteeAmount": 188635.69
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Loan top-up application created successfully",
  "data": {
    "newLoanId": 450,
    "newLoanNumber": "LN-20260728-450",
    "parentLoanId": 366,
    "totalAmount": 338635.69,
    "topupAmount": 200000.00,
    "settlementAmount": 138635.69,
    "status": "PENDING_GUARANTOR_APPROVAL",
    "applicationDate": "2026-07-28T14:30:00"
  }
}
```

### 2. Get Top-Up Preview/Calculation

**GET** `/api/loans/{loanId}/topup-preview?amount=200000&termMonths=48`

**Response:**
```json
{
  "parentLoanId": 366,
  "parentOutstanding": 138635.69,
  "topupAmount": 200000.00,
  "totalNewLoanAmount": 338635.69,
  "interestRate": 12.00,
  "termMonths": 48,
  "totalInterest": 162544.93,
  "totalRepayable": 501180.62,
  "monthlyRepayment": 10441.26,
  "eligibilityCheck": {
    "eligible": true,
    "maxAmount": 500000.00,
    "errors": [],
    "warnings": []
  }
}
```

### 3. Get Loan with Top-Up History

**GET** `/api/loans/{loanId}?includeTopupHistory=true`

**Response includes:**
```json
{
  "id": 366,
  "status": "REPAID",
  "settledViaTopup": true,
  "topupLoanId": 450,
  "topupDetails": {
    "topupLoanNumber": "LN-20260728-450",
    "topupDate": "2026-07-28T14:30:00",
    "settlementAmount": 138635.69,
    "additionalAmount": 200000.00,
    "totalNewLoan": 338635.69
  }
}
```

---

## Database Schema

### Loans Table (New Fields)

```sql
ALTER TABLE loans ADD COLUMN parent_loan_id BIGINT NULL;
ALTER TABLE loans ADD COLUMN is_topup BOOLEAN DEFAULT FALSE;
ALTER TABLE loans ADD COLUMN topup_amount DECIMAL(15,2) NULL;
```

**Field Descriptions:**

- `parent_loan_id`: References the original loan that was topped up
- `is_topup`: Flag to quickly identify top-up loans
- `topup_amount`: The additional new money (excluding settlement amount)

**Example Data:**

Old Loan (ID: 366):
```
id: 366
amount: 329297.00
outstanding_balance: 0.00  (after topup)
status: REPAID
parent_loan_id: NULL
is_topup: FALSE
```

New Loan (ID: 450):
```
id: 450
amount: 338635.69
outstanding_balance: 338635.69
status: DISBURSED
parent_loan_id: 366
is_topup: TRUE
topup_amount: 200000.00
```

---

## Accounting Logic

### When Top-Up Loan is DISBURSED:

**Journal Entries:**

```
1. Clear the old loan from books:
   DR: Loan #366 Account (Asset)          138,635.69
   CR: Loans Receivable                    138,635.69

2. Record new loan receivable:
   DR: Loans Receivable                    338,635.69
   CR: Loan #450 Account (Asset)          338,635.69

3. Disburse new money to member:
   DR: Member Loan Account                 200,000.00
   CR: Cash/Bank                           200,000.00

4. Settle old loan internally:
   DR: Loan #450 Disbursement             138,635.69
   CR: Loan #366 Settlement               138,635.69
```

**Net Effect:**
- Old Loan #366: Fully settled (0 balance)
- New Loan #450: Outstanding KES 338,635.69
- Member receives: KES 200,000.00 in cash
- **System remains 100% accurate**

---

## Business Rules

### 1. **Eligibility for Top-Up**

A loan can be topped up if:
- ✅ Current status is DISBURSED
- ✅ Outstanding balance > 0
- ✅ Member has no active pending loans
- ✅ Member meets eligibility for NEW total amount
- ✅ No active suspensions or exits

### 2. **Guarantor Handling**

- Old guarantors are **automatically released** when parent loan is settled
- New loan requires **fresh guarantors**
- Guarantors can be same people or different
- They must approve the NEW loan amount

### 3. **Interest Calculation**

- Interest is calculated on **TOTAL new loan amount**
- Example: KES 338,635.69 × 12% × 4 years = KES 162,544.93
- No "rollover" of old interest - fresh start

### 4. **Repayment**

- Monthly payments calculated on new total: KES 10,441.26
- No distinction between "old" and "new" portions
- All payments reduce the single new loan balance

### 5. **Approval Workflow**

Top-up loans follow **normal loan approval process**:
1. PENDING_GUARANTOR_APPROVAL
2. PENDING_LOAN_OFFICER_REVIEW
3. PENDING_CREDIT_COMMITTEE
4. PENDING_TREASURER
5. APPROVED → DISBURSED

---

## Reports & Tracking

### Top-Up Report

Show all top-up loans with:
- Parent loan details
- Top-up amount vs settlement amount
- New loan status
- Member information

### Member Loan History

When viewing member loans, clearly mark:
```
Loan #366 - Normal Loan - KES 329,297
  Status: REPAID (via top-up)
  ↓ Topped up to Loan #450

Loan #450 - Normal Loan - KES 338,635.69
  Status: DISBURSED
  ↑ Top-up of Loan #366 (Added: KES 200,000)
```

---

## Benefits

### For Members:
- ✅ **Consolidate debt** with additional funds in one loan
- ✅ **Fresh start** with new repayment schedule
- ✅ **Clear tracking** of what's settled vs new money
- ✅ **Simple process** - apply like a regular loan

### For SACCO:
- ✅ **100% accurate** accounting - no shortcuts
- ✅ **Complete audit trail** - every step documented
- ✅ **Fresh guarantor commitments** for new loan
- ✅ **Normal approval workflow** - no special cases
- ✅ **Easy reporting** - clearly marked top-ups

### For Treasurers:
- ✅ **Transparent breakdown** of settlement vs new funds
- ✅ **Clear link** between old and new loans
- ✅ **Accurate GL entries** for reconciliation
- ✅ **No manual calculations** - system handles it all

---

## Implementation Checklist

### Backend:
- [x] Add database fields (parent_loan_id, is_topup, topup_amount)
- [ ] Create migration file V144
- [ ] Update Loan entity
- [ ] Create LoanTopUpRequest DTO
- [ ] Implement top-up service logic
- [ ] Add top-up endpoint to LoanController
- [ ] Add top-up preview endpoint
- [ ] Update loan settlement logic
- [ ] Add audit logging for top-ups
- [ ] Test accounting entries

### Frontend:
- [ ] Add "Top-Up This Loan" button on loan details page
- [ ] Create top-up application dialog
- [ ] Show calculation preview
- [ ] Add guarantor selection for top-up
- [ ] Display top-up history on loan details
- [ ] Show parent/child loan relationships
- [ ] Add top-up indicator badges
- [ ] Test user flow end-to-end

### Documentation:
- [x] Create this comprehensive guide
- [ ] Add API documentation
- [ ] Create user training materials
- [ ] Update treasurer manual

---

## FAQ

### Q: What happens to the old loan's repayments?
**A:** They're preserved in the system. The loan shows as "REPAID via top-up" with full history intact.

### Q: Can I top-up a top-up?
**A:** Yes! You can top-up any DISBURSED loan, including previous top-ups. Each maintains its parent link.

### Q: Do old guarantors need to approve the top-up?
**A:** No. They're released when the old loan is settled. New guarantors must approve the new loan.

### Q: How is interest calculated?
**A:** Fresh calculation on the total new amount. Example: KES 338,635.69 × 12% × 4 years.

### Q: Can I reduce the loan amount during top-up?
**A:** No. Top-up = Outstanding Balance + Additional Amount. It always increases.

### Q: What if the top-up is rejected?
**A:** Old loan continues as normal. Nothing changes until top-up is approved and disbursed.

---

## Technical Notes

- Use transactions for top-up operations
- Lock parent loan during top-up creation
- Validate eligibility before creating new loan
- Generate proper audit trail at each step
- Use BigDecimal for all monetary calculations
- Test edge cases (minimum top-up, maximum eligibility, etc.)

---

**Last Updated:** 2026-07-28
**Version:** 1.0
