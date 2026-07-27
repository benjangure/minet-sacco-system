# GL Setup & Reporting Guide: Complete Step-by-Step Workflow

## Overview

Your GL system has **two-phase workflow**:

1. **Phase 1: Configuration** - Set up which GL accounts appear in which reports (DONE ✅)
2. **Phase 2: Data Entry** - Add actual journal entries that populate the reports

This guide walks you through both phases with the ideal workflow.

---

## Phase 1: GL Account Configuration (Already Complete)

### Current Setup

Your GL accounts are already configured with the following structure:

```
ASSETS
├── Loans (AGGREGATION - auto-calculated from loans table)
│   ├── Normal Loans
│   ├── Emergency Loan Type 1
│   ├── Emergency Loan Type 2
├── Bank Accounts (AGGREGATION - auto-calculated)
│   ├── CBA Call Deposits
│   ├── CBA Current Account
├── Investments (MANUAL_ENTRY - requires journal entries)
│   ├── Co-op Holdings
│   ├── Co-op Insurance
│   ├── KUSCCO
│   └── Receivables

LIABILITIES
├── Member Deposits (AGGREGATION - auto-calculated from accounts)
├── Member Shares (AGGREGATION - auto-calculated)
└── Payables (MANUAL_ENTRY)
    ├── Auditor Fees Payable
    ├── Dividend Payable
    ├── Interest Payable
    └── Committee Allowance Payable

EQUITY
├── Statutory Reserve (MANUAL_ENTRY)
├── Revenue Reserve (MANUAL_ENTRY)
└── Retained Earnings (COMPUTED - calculated)

REVENUE (for Income Statement)
├── Interest - Loans (AGGREGATION - auto-calculated)
├── Interest - Deposits (MANUAL_ENTRY)
├── Entrance Fees (AGGREGATION - auto-calculated)
└── Loan Processing Fees (MANUAL_ENTRY)

EXPENSE (for Income Statement)
├── Audit Fees (MANUAL_ENTRY)
├── Travel Expenses (MANUAL_ENTRY)
├── SASRA Fees (MANUAL_ENTRY)
├── Training (MANUAL_ENTRY)
├── Committee Allowances (MANUAL_ENTRY)
├── AGM Expenses (MANUAL_ENTRY)
├── Insurance Premiums (MANUAL_ENTRY)
├── Bank Charges (AGGREGATION)
├── Loan Loss Provision (MANUAL_ENTRY)
├── Income Tax (MANUAL_ENTRY)
└── Interest Expense (MANUAL_ENTRY)
```

### How Reports Map to GL Accounts

#### Balance Sheet (Point-in-Time)
```
ASSETS (All ASSET accounts)
= 
LIABILITIES (All LIABILITY accounts) + EQUITY (All EQUITY accounts)
```

**Example:**
```
Assets:
  Normal Loans:           500,000
  CBA Current Account:     50,000
  Co-op Holdings:         100,000
  ──────────────────────
  Total Assets:           650,000

Liabilities:
  Member Deposits:        400,000
  Member Shares:          100,000
  Auditor Fees Payable:     5,000
  ──────────────────────
  Total Liabilities:      505,000

Equity:
  Statutory Reserve:       75,000
  Revenue Reserve:         25,000
  Retained Earnings:       45,000
  ──────────────────────
  Total Equity:           145,000

Check: 505,000 + 145,000 = 650,000 ✅
```

#### Income Statement (Period: January 2026)
```
REVENUES (All REVENUE accounts)
-
EXPENSES (All EXPENSE accounts)
=
NET INCOME / NET LOSS
```

**Example:**
```
Revenues:
  Interest - Loans:       150,000
  Interest - Deposits:     10,000
  Entrance Fees:            2,000
  Loan Processing Fees:     5,000
  ──────────────────────
  Total Revenues:         167,000

Expenses:
  Audit Fees:             10,000
  SASRA Fees:              5,000
  Travel Expenses:         3,000
  Committee Allowances:    8,000
  Insurance Premiums:      2,000
  Bank Charges:              500
  Income Tax:              5,000
  ──────────────────────
  Total Expenses:          33,500

Net Income:               133,500 (167,000 - 33,500)
```

---

## Phase 2: Adding Journal Entries (This Is Where You Are Now)

### 2.1 Understanding the Two Types of GL Accounts

#### Type A: AGGREGATION Accounts (Auto-Calculated ✅ No Manual Entry Needed)
These pull data automatically from operational tables:

```
Account Type          Source Table    Pulls What
─────────────────────────────────────────────────────
Normal Loans          loans table     Outstanding balance of normal loans
Emergency Loans       loans table     Outstanding balance by emergency type
Member Deposits       accounts table  Savings account balances
Member Shares         accounts table  Share account balances
Interest - Loans      transactions    Interest amounts
Entrance Fees         transactions    Entrance fee transactions
Bank Charges          transactions    Bank charge transactions
```

✅ **You don't need to create journal entries for these** - they update automatically.

#### Type B: MANUAL_ENTRY Accounts (Require Journal Entries)
These need you to manually create journal entries:

```
ASSETS that need entries:
  - Co-op Holdings (shareholdings in cooperative societies)
  - Co-op Insurance (life insurance through cooperative)
  - KUSCCO (deposits in KUSCCO account)
  - Receivables (money owed to the SACCO)

LIABILITIES that need entries:
  - Auditor Fees Payable (accrued auditor costs)
  - Dividend Payable (accrued dividends)
  - Interest Payable (accrued interest on member deposits)
  - Committee Allowance Payable (accrued committee pay)

EQUITY that needs entries:
  - Statutory Reserve (retained earnings allocated by law)
  - Revenue Reserve (retained earnings allocated by policy)
  - Retained Earnings (calculated from net income)

REVENUE that needs entries:
  - Interest - Deposits (if manual calculation)
  - Loan Processing Fees (if manual tracking)

EXPENSES that need entries:
  - All manual expense accounts
```

---

## Phase 3: Ideal Workflow for Adding Journal Entries

### Step 1: Identify What Journal Entries You Need

Start by asking: **"What are my organization's unique transactions that aren't in the standard tables?"**

#### Common Examples for Kenyan SACCOs:

```
MONTHLY ACCRUALS (Do These Every Month)
├─ Interest Payable
│  └─ "We earned interest on member deposits but haven't paid it yet"
│     Journal Entry: Debit Interest-Deposits / Credit Interest Payable
│
├─ Auditor Fees Payable
│  └─ "Auditor invoiced us but we haven't paid yet"
│     Journal Entry: Debit Audit Fees / Credit Auditor Fees Payable
│
└─ Provision for Loan Losses
   └─ "We estimate 5% of loans might not be repaid"
      Journal Entry: Debit Loan Loss Provision / Credit Loan Loss Reserve

PERIOD-END ADJUSTMENTS (Do These Quarterly/Annually)
├─ Dividend Payable
│  └─ "Board approved dividend distribution"
│     Journal Entry: Debit Retained Earnings / Credit Dividend Payable
│
├─ Statutory Reserve
│  └─ "By law, we must retain 20% of profits"
│     Journal Entry: Debit Retained Earnings / Credit Statutory Reserve
│
└─ Loan Loss Reserve Write-off
   └─ "Loans written off exceeded provision estimate"
      Journal Entry: Debit Loan Loss Provision / Credit Loans

INVESTMENT TRANSACTIONS
├─ Co-op Holdings
│  └─ "Purchased shares in agricultural cooperative"
│     Journal Entry: Debit Co-op Holdings / Credit CBA Current Account
│
├─ Co-op Insurance
│  └─ "Paid life insurance premium through cooperative"
│     Journal Entry: Debit Co-op Insurance / Credit CBA Current Account
│
└─ KUSCCO Deposit
   └─ "Deposited funds in KUSCCO"
      Journal Entry: Debit KUSCCO / Credit CBA Current Account
```

### Step 2: Create the Journal Entry (Via UI or API)

#### Using the UI (Recommended for Treasurers)

1. Navigate to **GL Manual Entries** page
2. Click **"New Entry"** button
3. Fill in:
   ```
   GL Account:        Interest - Deposits (select from dropdown)
   Entry Type:        ACCRUAL
   Amount:            15,000.00 KES
   Entry Reason:      Monthly interest accrual on member deposits
   Description:       Based on average deposit balance of 500,000 at 3.6% annual rate
   ```
4. System creates entry in **PENDING** status
5. Click **"Submit"** when ready
6. Entry moves to **POSTED** (awaiting admin approval)

#### Using the API (For Integration)

```bash
POST /api/gl/manual-entries

Request Body:
{
  "glAccountCode": "INT_DEPOSITS",
  "entryType": "ACCRUAL",
  "amount": 15000.00,
  "entryReason": "Monthly interest accrual on member deposits",
  "description": "Based on average deposit balance of 500,000 at 3.6% annual rate"
}

Response:
{
  "id": 42,
  "glAccountCode": "INT_DEPOSITS",
  "amount": 15000.00,
  "periodStatus": "POSTED",
  "approvalStatus": "PENDING",
  "createdBy": "john_treasurer"
}
```

### Step 3: Admin Reviews & Approves (Part of Maker-Checker)

1. Admin navigates to **GL Manual Entries**
2. Clicks on **"Pending Approval"** tab
3. Reviews entry details
4. Clicks **✅ Approve** or **❌ Reject**

**If Approved:**
- Entry moves to **APPROVED** status
- Amount immediately affects the GL calculation
- Next Balance Sheet/Income Statement will include this amount

**If Rejected:**
- Entry returns to **DRAFT** for treasurer to revise
- Treasurer can edit and resubmit
- Cycle repeats

---

## Phase 4: How Amounts Flow to Reports

### Balance Sheet Example

**You create this journal entry (January 5):**
```
Debit: Co-op Holdings    15,000 KES
Credit: CBA Current      15,000 KES
Reason: Purchased shares in agricultural cooperative
```

**Balance Sheet as of January 31:**
```
ASSETS
  CBA Current Account:  185,000 KES  (was 200,000 - paid 15,000)
  Co-op Holdings:        15,000 KES  (was 0 - added 15,000)
  ─────────────────────
  Total Assets (updated)

The entry is now reflected in the report ✅
```

### Income Statement Example

**You create this journal entry (January 31):**
```
Debit: Interest Expense       25,000 KES
Credit: Interest Payable      25,000 KES
Reason: Monthly interest accrued on member deposits
```

**Income Statement for January 2026:**
```
REVENUES:                                165,000 KES
EXPENSES:
  Interest Expense:      25,000 KES  ← YOUR ENTRY
  Other Expenses:        40,000 KES
  ──────────────────────
  Total Expenses:        65,000 KES

Net Income:              100,000 KES (165,000 - 65,000)
```

---

## Complete Monthly Close Procedure (Best Practice)

### Week 1: First Business Day of New Month

**Morning:**
1. Treasurer reviews all pending transactions from last month
2. Identifies what needs accrual/adjustment
3. Creates preliminary list

**Afternoon:**
1. Finance committee meeting
2. Reviews and approves proposed journal entries
3. Treasurer enters entries in GL system

### Week 2: Mid-Month Review

**Task:**
1. Check for any transaction posting errors
2. Verify all manual entries are in system
3. Run Trial Balance report
4. Ensure it balances

**If Not Balanced:**
- Identify discrepancies
- Create corrective journal entries
- Get approval and post

### Week 3-4: Period Close

**Day 1 - Entry Cutoff:**
- No new entries accepted after this date
- Allows admin time to review

**Day 2-3 - Admin Approval:**
- Admin approves all pending entries
- Rejects any that don't meet policy

**Day 4 - Report Generation:**
- Generate Balance Sheet
- Generate Income Statement
- Generate Trial Balance
- Verify accounting equation: Assets = Liabilities + Equity

**Day 5 - Review & Finalization:**
- Finance Committee reviews reports
- Discuss variance from prior period
- Approve for external reporting

### Day 6 - External Reporting:
- Submit to SASRA (if required)
- Share with board
- Archive for audit

---

## Recommended GL Entry Templates by Account Type

### Monthly Recurring Entries (Create Them Every Month)

#### Template 1: Interest Accrual
```
Account Type:     ACCRUAL
GL Account:       Interest Payable / Interest Deposits
Amount:           [Calculate: (Total Member Deposits × Annual Rate%) / 12]
Frequency:        Monthly (last business day)
Approval:         CFO
Supporting Doc:   Member account listing with interest calculation
```

**Example Calculation:**
```
Member Deposits:    5,000,000 KES
Annual Interest:    3.6%
Monthly Interest:   (5,000,000 × 3.6%) / 12 = 15,000 KES

Entry:
Debit Interest-Deposits     15,000
Credit Interest Payable     15,000
```

#### Template 2: Provision for Loan Losses
```
Account Type:     ACCRUAL
GL Account:       Loan Loss Provision
Amount:           [Calculate: Outstanding Loans × Risk %]
Frequency:        Monthly or Quarterly
Approval:         CEO/CFO
Supporting Doc:   Loan portfolio analysis with risk assessment
```

**Example Calculation:**
```
Outstanding Normal Loans:        2,000,000 KES
Outstanding Emergency Loans:       500,000 KES
Combined Portfolio:              2,500,000 KES
Risk Percentage:                 5% (based on historical defaults)
Provision Needed:                (2,500,000 × 5%) = 125,000 KES

Current Provision Balance:       100,000 KES
Additional Provision Needed:      25,000 KES

Entry:
Debit Loan Loss Provision        25,000
Credit Loan Loss Reserve         25,000
```

#### Template 3: Committee Allowances Accrual
```
Account Type:     ACCRUAL
GL Account:       Committee Allowances Payable
Amount:           [Calculate: Number of Committee Members × Monthly Allowance]
Frequency:        Monthly (last business day)
Approval:         Board Secretary
Supporting Doc:   Committee member list with approved allowance rates
```

**Example Calculation:**
```
Committee Members:           7
Approved Monthly Allowance:  2,000 KES per member
Total Monthly Accrual:       7 × 2,000 = 14,000 KES

Entry:
Debit Committee Allowances        14,000
Credit Committee Allowance Pay    14,000
```

#### Template 4: Auditor Fees Accrual
```
Account Type:     ACCRUAL
GL Account:       Audit Fees / Auditor Fees Payable
Amount:           [Based on auditor engagement letter]
Frequency:        Monthly (accrual) or as invoiced
Approval:         Finance Committee
Supporting Doc:   Auditor engagement letter or invoice
```

**Example Calculation:**
```
Annual Audit Fee:           120,000 KES
Monthly Accrual:            120,000 / 12 = 10,000 KES

Entry:
Debit Audit Fees             10,000
Credit Auditor Fees Pay      10,000
```

### Quarterly Entries (Create at Period End)

#### Template 5: Depreciation / Fixed Asset Adjustment
```
Account Type:     ADJUSTMENT
GL Account:       Various (based on asset)
Amount:           [Based on depreciation schedule]
Frequency:        Quarterly
Approval:         Finance Committee
Supporting Doc:   Fixed asset register with depreciation schedule
```

#### Template 6: Dividend Accrual (If Approved)
```
Account Type:     ACCRUAL
GL Account:       Dividend Payable
Amount:           [Calculated from net income and board resolution]
Frequency:        Quarterly or as approved
Approval:         Board
Supporting Doc:   Board resolution approving dividend
```

**Example Calculation:**
```
Net Income Last Quarter:    500,000 KES
Dividend Payout Ratio:      40% (approved by board)
Dividend to Accrue:         500,000 × 40% = 200,000 KES

Entry:
Debit Retained Earnings        200,000
Credit Dividend Payable        200,000
```

### Annual Entries (Create at Year End)

#### Template 7: Statutory Reserve Appropriation
```
Account Type:     ADJUSTMENT
GL Account:       Statutory Reserve
Amount:           [Based on law and policy: typically 20% of net income]
Frequency:        Annual (December/January)
Approval:         Board
Supporting Doc:   Board resolution + applicable SACCO regulations
```

**Example Calculation (Assuming Kenyan SACCO Law):**
```
Annual Net Income:          1,000,000 KES
Statutory Reserve %:        20%
Amount to Reserve:          1,000,000 × 20% = 200,000 KES

Entry:
Debit Retained Earnings        200,000
Credit Statutory Reserve       200,000
```

#### Template 8: Revenue Reserve Appropriation (If Policy Allows)
```
Account Type:     ADJUSTMENT
GL Account:       Revenue Reserve
Amount:           [Based on board policy]
Frequency:        Annual or as approved
Approval:         Board
Supporting Doc:   Board resolution
```

---

## Step-by-Step Example: A Complete Month's Close

### Scenario: December 2025 Close (for January 2026 reports)

#### Morning - Dec 31, 2025 (Treasurer)

**Task 1: Interest Accrual**
```
1. Check member deposit balances in system: 5,200,000 KES
2. Annual interest rate: 3.6%
3. Monthly accrual: (5,200,000 × 3.6%) / 12 = 15,600 KES

Navigate to GL > New Manual Entry
  Account:    Interest - Deposits
  Type:       ACCRUAL
  Amount:     15,600 KES
  Reason:     December 2025 interest accrual
  
Click Submit → Entry now POSTED (awaiting approval)
```

**Task 2: Loan Loss Provision**
```
1. Total outstanding loans: 2,800,000 KES
2. Historical default rate: 3%
3. Provision needed: 2,800,000 × 3% = 84,000 KES
4. Current provision balance: 80,000 KES
5. Additional needed: 4,000 KES

Navigate to GL > New Manual Entry
  Account:    Loan Loss Provision
  Type:       ACCRUAL
  Amount:     4,000 KES
  Reason:     December 2025 loan loss provision adjustment
  
Click Submit → Entry now POSTED
```

**Task 3: Auditor Fees Accrual**
```
Annual audit fee: 120,000 KES
Monthly accrual: 10,000 KES

Navigate to GL > New Manual Entry
  Account:    Audit Fees
  Type:       ACCRUAL
  Amount:     10,000 KES
  Reason:     December 2025 auditor fees accrual
  
Click Submit → Entry now POSTED
```

**Task 4: Committee Allowances**
```
7 committee members × 2,000 KES = 14,000 KES

Navigate to GL > New Manual Entry
  Account:    Committee Allowances
  Type:       ACCRUAL
  Amount:     14,000 KES
  Reason:     December 2025 committee allowances accrual
  
Click Submit → Entry now POSTED
```

#### Afternoon - Dec 31, 2025 (Admin)

**Task: Review & Approve All Entries**

Navigate to GL > Pending Approvals
```
Entry 1: Interest Accrual         15,600 KES  ✅ Approve
Entry 2: Loan Loss Provision       4,000 KES  ✅ Approve
Entry 3: Auditor Fees Accrual     10,000 KES  ✅ Approve
Entry 4: Committee Allowances     14,000 KES  ✅ Approve
```

Each entry is now **APPROVED** and updates GL calculations.

#### Next Day - Jan 1, 2026 (Anyone)

**Task: Run Reports**

**1. Generate Balance Sheet (as of Dec 31, 2025)**
```
ASSETS                              Amount
─────────────────────────────────────────
Normal Loans                   1,500,000
Emergency Loans                  500,000
CBA Current Account              250,000
CBA Call Deposits                100,000
Co-op Holdings                   100,000
KUSCCO Deposit                     50,000
Receivables                        25,000
                         ──────────────────
TOTAL ASSETS                  2,525,000

LIABILITIES
─────────────────────────────────────────
Member Deposits                1,800,000
Member Shares                    400,000
Interest Payable                  15,600  ← NEW (from entry above)
Auditor Fees Payable              10,000  ← NEW
Committee Allow. Payable          14,000  ← NEW
                         ──────────────────
TOTAL LIABILITIES              2,239,600

EQUITY
─────────────────────────────────────────
Statutory Reserve                 50,000
Revenue Reserve                   30,000
Retained Earnings               (175,400) ← Calculated
                         ──────────────────
TOTAL EQUITY                    (95,400)

VERIFICATION: 2,239,600 + (-95,400) = 2,144,200
Wait... this doesn't balance!

ACTION: Review entries for errors or contact auditor.
```

**2. Generate Income Statement (Jan 1 - Dec 31, 2025)**
```
REVENUES                            Amount
─────────────────────────────────────────
Interest - Loans              180,000
Interest - Deposits            15,600  ← NEW (from accrual)
Entrance Fees                   3,000
Processing Fees                 5,000
                         ──────────────────
TOTAL REVENUE                 203,600

EXPENSES
─────────────────────────────────────────
Auditor Fees                   10,000  ← NEW
Audit Fees accrual             10,000  ← Wait, duplicate?
SASRA Fees                      5,000
Travel Expenses                 2,000
Committee Allowances           14,000  ← NEW
Training                        1,500
Insurance Premiums             2,000
Loan Loss Provision             4,000  ← NEW
Bank Charges                      500
Income Tax                      8,000
Interest Expense              15,000
                         ──────────────────
TOTAL EXPENSES                72,000

NET INCOME                    131,600
```

---

## Dashboard View - Real-Time Tracking

After entries are approved, your dashboard shows:

```
GL SUMMARY (Updated Real-Time)

Total Assets:         2,525,000 KES
Total Liabilities:    2,239,600 KES
Total Equity:            85,400 KES

Latest Approved Entries:
✅ Interest Accrual       15,600 KES  Dec 31, 2:45 PM
✅ Loan Loss Provision     4,000 KES  Dec 31, 2:50 PM
✅ Auditor Fees Accrual   10,000 KES  Dec 31, 3:00 PM
✅ Committee Allow.       14,000 KES  Dec 31, 3:05 PM

Pending Review:
(None currently)
```

---

## Audit Trail Verification

Every entry is tracked:

```
Query: Show all GL entries for December 2025

Timestamp           Action              User           Account               Amount      Status
─────────────────────────────────────────────────────────────────────────────────────────────
2025-12-31 14:30   GL_ENTRY_CREATED    john_treasurer Interest Deposits     15,600     DRAFT
2025-12-31 14:35   GL_ENTRY_SUBMITTED  john_treasurer Interest Deposits     15,600     POSTED
2025-12-31 15:45   GL_ENTRY_APPROVED   sarah_admin    Interest Deposits     15,600     APPROVED
2025-12-31 14:40   GL_ENTRY_CREATED    john_treasurer Loan Loss Prov.        4,000     DRAFT
2025-12-31 14:45   GL_ENTRY_SUBMITTED  john_treasurer Loan Loss Prov.        4,000     POSTED
2025-12-31 15:50   GL_ENTRY_APPROVED   sarah_admin    Loan Loss Prov.        4,000     APPROVED
(... more entries ...)
```

---

## Common Questions

### Q: When should I create journal entries?

**A: Depends on the account type:**

| Account Type | When to Enter | Frequency | Example |
|---|---|---|---|
| AGGREGATION | Never - automatic | Real-time | Loans, deposits |
| MANUAL_ENTRY | Monthly | Last business day | Accruals, expenses |
| MANUAL_ENTRY | Quarterly | Period end | Provisions, reserves |
| MANUAL_ENTRY | Annual | Year end | Statutory reserve |

### Q: Can I edit a journal entry after it's approved?

**A: No.** Once approved, entries are locked. If you need to correct it:
1. Create a **reversing entry** (opposite amounts)
2. Create a **correcting entry** (with right amounts)
3. Submit both for approval

**Example:**
```
Original (Incorrect) Entry:
Debit Interest Deposits     15,600
Credit Interest Payable     15,600

Realized it should be 16,200. Create:

Reversing Entry:
Debit Interest Payable      15,600
Credit Interest Deposits    15,600

Correcting Entry:
Debit Interest Deposits     16,200
Credit Interest Payable     16,200
```

### Q: What if I don't create a journal entry?

**A: Nothing breaks, but:**
- That GL account will show zero balance
- Reports will be incomplete
- You won't capture all financial activity
- Audit trail will show missing entries

### Q: How do I know my entries are correct?

**A: Use the Trial Balance report:**
```
Sum of all DEBIT accounts = Sum of all CREDIT accounts
If balanced → Your entries are correct ✅
If not → Find and fix the discrepancy ❌
```

### Q: Can multiple people create entries?

**A: Yes, with safeguards:**
- Any user with TREASURER role can create entries
- Only ADMIN role can approve/reject
- All actions logged in audit trail
- Supports team workflows

---

## Summary Workflow

```
1. IDENTIFY what needs to be recorded
   ↓
2. CALCULATE the amount
   ↓
3. CREATE the journal entry (Treasurer)
   ↓
4. SUBMIT for approval
   ↓
5. ADMIN REVIEWS & APPROVES
   ↓
6. AMOUNT APPEARS IN REPORTS
   ↓
7. AUDIT TRAIL LOGS EVERYTHING
```

**You're now ready to populate your GL with journal entries and generate accurate financial reports!**
