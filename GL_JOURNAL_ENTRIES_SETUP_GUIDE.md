# GL Journal Entries Setup & Mapping Guide

## Overview: The Complete Workflow

This guide walks you through the **entire process** of setting up and using journal entries in your SACCO's GL system. The flow is:

```
1. Configure GL Accounts (Chart of Accounts)
   ↓
2. Map GL Accounts to Reports
   ↓
3. Create Journal Entries (Treasurer)
   ↓
4. Approve Journal Entries (Admin)
   ↓
5. GL System Calculates Balances
   ↓
6. Reports Auto-Generated with Approved Entries
```

---

## PHASE 1: Configure GL Accounts (Chart of Accounts)

This is the **ONE-TIME SETUP** where you define all your accounts and how they calculate.

### Step 1.1: Understand the Account Types

Your GL system supports **5 account types**, each serving a different purpose:

#### 1. **ASSET Accounts** (Left side of balance sheet)
What the SACCO **owns**:
- Bank deposits (CBA_CALL_DEPOSITS, CBA_CURRENT)
- Loans outstanding to members (LOAN_NORMAL, LOAN_EMERGENCY_1, etc.)
- Investments (CO_OP_HOLDINGS, COOP_INSURANCE, KUSCCO)
- Receivables

**Key Point:** Asset accounts appear on the **Balance Sheet** as debit balances (normal debit balance).

#### 2. **LIABILITY Accounts** (Right side of balance sheet)
What the SACCO **owes**:
- Member deposits (MEMBER_DEPOSITS)
- Member shares (MEMBER_SHARES)
- Fees/dividends payable
- Interest payable
- Committee allowances payable

**Key Point:** Liability accounts appear on **Balance Sheet** as credit balances (normal credit balance).

#### 3. **EQUITY Accounts** (Right side of balance sheet)
**Ownership interest**:
- Statutory Reserve
- Revenue Reserve
- Retained Earnings

**Key Point:** Equity accounts appear on **Balance Sheet** (computed from net income).

#### 4. **REVENUE Accounts** (Income Statement - increases net income)
What generates **income**:
- Interest from loans (INT_LOANS)
- Interest on deposits (INT_DEPOSITS)
- Entrance fees (ENTRANCE_FEES)
- Loan processing fees (LOAN_PROCESSING_FEE)

**Key Point:** Revenue accounts appear on **Income Statement** as credit balances (normal credit = increase income).

#### 5. **EXPENSE Accounts** (Income Statement - decreases net income)
What **costs money**:
- Audit fees (AUDIT_FEES)
- Travel expenses (TRAVEL_EXPENSES)
- SASRA fees (SASRA_FEES)
- Training, committee allowances
- Insurance premiums, bank charges
- Loan loss provision
- Income tax, interest expense

**Key Point:** Expense accounts appear on **Income Statement** as debit balances (normal debit = increase expenses).

### Step 1.2: How GL Accounts Calculate Their Balance

Each GL account has a **calculation method**. There are **3 types**:

#### Type 1: **AGGREGATION** (Auto-calculated from operational data)
```
The system automatically pulls data from operational tables.
Example: LOAN_NORMAL account pulls all outstanding balances 
         from loans table where loan_type='NORMAL' and status='DISBURSED'

Configuration:
{
  "table": "loans",
  "field": "outstanding_balance",
  "where": "loan_type = 'NORMAL' AND status = 'DISBURSED'"
}

Result: This account ALWAYS has the correct balance (no manual entries needed)
        It's updated automatically when loans change.
```

**Accounts using AGGREGATION:**
- LOAN_NORMAL, LOAN_EMERGENCY_1, LOAN_EMERGENCY_2 (from loans table)
- MEMBER_DEPOSITS, MEMBER_SHARES (from accounts table)
- INT_LOANS, ENTRANCE_FEES, BANK_CHARGES (from transactions table)

#### Type 2: **MANUAL_ENTRY** (Requires journal entries)
```
The system starts at ZERO and waits for treasurer to enter journal entries.
Example: AUDIT_FEES - No automatic data source exists,
         so treasurer must manually record when audit is paid.

Configuration:
{
  "type": "manual"
}

Result: This account's balance is built ONLY from approved journal entries.
        System calculates: Sum of all approved DEBIT entries - Sum of all CREDIT entries
```

**Accounts using MANUAL_ENTRY (You'll enter most data here):**
- CO_OP_HOLDINGS, COOP_INSURANCE, KUSCCO (investments)
- RECEIVABLES (if not auto-tracked)
- AUDITOR_PAYABLE, DIVIDEND_PAYABLE, INTEREST_PAYABLE, COMMITTEE_ALLOWANCE_PAYABLE
- STATUTORY_RESERVE, REVENUE_RESERVE
- INT_DEPOSITS, LOAN_PROCESSING_FEE
- AUDIT_FEES, TRAVEL_EXPENSES, SASRA_FEES, TRAINING, COMMITTEE_ALLOWANCES, AGM_EXPENSES, INSURANCE_PREMIUMS, LOAN_LOSS_PROVISION, INCOME_TAX, INTEREST_EXPENSE

#### Type 3: **COMPUTED** (Calculated from other accounts)
```
The system computes this from a formula.
Example: RETAINED_EARNINGS = Previous Retained Earnings + Current Year Net Income

Configuration:
{
  "type": "computed"
}

Result: You don't enter anything; the system calculates this automatically.
```

**Accounts using COMPUTED:**
- RETAINED_EARNINGS

---

## PHASE 2: Verify GL Accounts Are Properly Configured

Your accounts are already set up in `V117__Populate_GL_Accounts.sql`. **Verify they exist:**

### Frontend: Check GL Account Configuration
1. Login as **ADMIN**
2. Go to **GL Configuration** → **Chart of Accounts**
3. Verify all these accounts are present and ACTIVE:

```
ASSETS:
✓ LOAN_NORMAL, LOAN_EMERGENCY_1, LOAN_EMERGENCY_2
✓ CBA_CALL_DEPOSITS, CBA_CURRENT
✓ CO_OP_HOLDINGS, COOP_INSURANCE, KUSCCO, RECEIVABLES

LIABILITIES:
✓ MEMBER_DEPOSITS, MEMBER_SHARES
✓ AUDITOR_PAYABLE, DIVIDEND_PAYABLE, INTEREST_PAYABLE, COMMITTEE_ALLOWANCE_PAYABLE

EQUITY:
✓ STATUTORY_RESERVE, REVENUE_RESERVE, RETAINED_EARNINGS

REVENUES:
✓ INT_LOANS, INT_DEPOSITS, ENTRANCE_FEES, LOAN_PROCESSING_FEE

EXPENSES:
✓ AUDIT_FEES, TRAVEL_EXPENSES, SASRA_FEES, TRAINING, COMMITTEE_ALLOWANCES, AGM_EXPENSES
✓ INSURANCE_PREMIUMS, BANK_CHARGES, LOAN_LOSS_PROVISION, INCOME_TAX, INTEREST_EXPENSE
```

**If ANY account is missing or INACTIVE:**
- Contact your IT team to activate it via the GL Configuration page
- Or manually update the database:
  ```sql
  UPDATE gl_accounts SET is_active = 1 WHERE code = 'ACCOUNT_CODE';
  ```

---

## PHASE 3: Understand Which Entries Go into Which Reports

Now you understand **how** accounts calculate. Here's **WHERE they appear**:

### Report 1: **BALANCE SHEET** (As of a specific date)

Shows the financial position at a moment in time: **Assets = Liabilities + Equity**

**What appears:**
- ✅ **ASSET accounts** (left column) - Starting balances
- ✅ **LIABILITY accounts** (right column) - What you owe
- ✅ **EQUITY accounts** (right column) - Ownership
- ❌ **REVENUE/EXPENSE accounts** - **NOT on balance sheet** (they affect equity via net income)

**Example Balance Sheet:**
```
ASSETS                              LIABILITIES
Loans Outstanding      500,000     Member Deposits      1,000,000
Bank Deposits          200,000     Member Shares          150,000
Investments            100,000     Payables                50,000
                                   TOTAL LIABILITIES   1,200,000

TOTAL ASSETS         800,000       EQUITY
                                   Statutory Reserve    100,000
                                   Revenue Reserve      150,000
                                   Retained Earnings   (650,000)
                                   TOTAL EQUITY        (400,000)
                    
Balance Check: 800,000 = 1,200,000 - 400,000  ✗ NOT BALANCED (example)
```

**GL Accounts that feed Balance Sheet:**
- **Auto-calculated (AGGREGATION):** LOAN_NORMAL, LOAN_EMERGENCY_1, LOAN_EMERGENCY_2, CBA_CALL_DEPOSITS, CBA_CURRENT, MEMBER_DEPOSITS, MEMBER_SHARES
- **Manual entries required:** CO_OP_HOLDINGS, COOP_INSURANCE, KUSCCO, RECEIVABLES, AUDITOR_PAYABLE, DIVIDEND_PAYABLE, INTEREST_PAYABLE, COMMITTEE_ALLOWANCE_PAYABLE, STATUTORY_RESERVE, REVENUE_RESERVE

---

### Report 2: **INCOME STATEMENT** (For a time period)

Shows financial performance: **Net Income = Total Revenues - Total Expenses**

**What appears:**
- ✅ **REVENUE accounts** - All income sources
- ✅ **EXPENSE accounts** - All costs
- ❌ **ASSET/LIABILITY/EQUITY accounts** - **NOT on income statement**

**Example Income Statement (January 2026):**
```
REVENUES
Interest on Loans          50,000
Entrance Fees              5,000
Processing Fees            3,000
TOTAL REVENUES            58,000

EXPENSES
Audit Fees                 10,000
Travel                      3,000
SASRA Fees                  2,000
Committee Allowances        1,500
Insurance                   1,000
Bank Charges                  500
TOTAL EXPENSES            18,000

NET INCOME                 40,000
```

**GL Accounts that feed Income Statement:**
- **Auto-calculated (AGGREGATION):** INT_LOANS, ENTRANCE_FEES, BANK_CHARGES
- **Manual entries required:** INT_DEPOSITS, LOAN_PROCESSING_FEE, AUDIT_FEES, TRAVEL_EXPENSES, SASRA_FEES, TRAINING, COMMITTEE_ALLOWANCES, AGM_EXPENSES, INSURANCE_PREMIUMS, LOAN_LOSS_PROVISION, INCOME_TAX, INTEREST_EXPENSE

---

### Report 3: **TRIAL BALANCE** (As of a specific date)

Shows all GL accounts and their balances (for reconciliation/audit):

**What appears:**
- ✅ **ALL accounts** - Assets, Liabilities, Equity, Revenues, Expenses

**Used for:** Verifying your books are balanced and checking for data entry errors.

---

## PHASE 4: Enter Journal Entries (The Treasurer's Job)

Now that you understand the accounts and reports, you're ready to **enter real data**.

### Step 4.1: Identify What Needs Manual Entries

**These accounts need MANUAL journal entries** (they don't auto-calculate):

#### BALANCE SHEET ITEMS (appear on Balance Sheet):
```
ASSET MANUAL ACCOUNTS:
- CO_OP_HOLDINGS (Co-op investments)
- COOP_INSURANCE (Insurance receivables)
- KUSCCO (KUSCCO holdings)
- RECEIVABLES (Any other receivables)

LIABILITY MANUAL ACCOUNTS:
- AUDITOR_PAYABLE (Auditors' fees owed but not yet paid)
- DIVIDEND_PAYABLE (Dividends owed to members)
- INTEREST_PAYABLE (Interest owed but not yet paid)
- COMMITTEE_ALLOWANCE_PAYABLE (Allowances owed to committee)

EQUITY MANUAL ACCOUNTS:
- STATUTORY_RESERVE (Mandatory reserve required by law)
- REVENUE_RESERVE (Optional reserve from profits)
```

#### INCOME STATEMENT ITEMS (appear on Income Statement):
```
REVENUE MANUAL ACCOUNTS:
- INT_DEPOSITS (Interest paid to savers' deposits)
- LOAN_PROCESSING_FEE (Processing fees collected)

EXPENSE MANUAL ACCOUNTS:
- AUDIT_FEES (Paid to external auditors)
- TRAVEL_EXPENSES (Staff travel costs)
- SASRA_FEES (Regulatory fees to SASRA)
- TRAINING (Staff training costs)
- COMMITTEE_ALLOWANCES (Payments to committee members)
- AGM_EXPENSES (Annual General Meeting costs)
- INSURANCE_PREMIUMS (Insurance premiums paid)
- LOAN_LOSS_PROVISION (Reserve for bad loans)
- INCOME_TAX (Corporate income tax)
- INTEREST_EXPENSE (Interest paid on deposits/borrowings)
```

### Step 4.2: Frontend - Create a Journal Entry

**As Treasurer, here's what you do:**

1. **Navigate to GL Manual Entries Page**
   - URL: `/gl-manual-entries`
   - You'll see a page titled "GL Manual Entries"

2. **Click "New Entry" Button**
   - A modal form opens

3. **Fill in the Entry Details**

```
Form Fields:

┌─────────────────────────────────────────────────┐
│ GL Account          : [Dropdown - Select Account]│
│ Entry Date          : [Date Picker]             │
│ Amount              : [Currency Input]          │
│ Is Debit?           : [Toggle Yes/No]           │
│ Entry Reason        : [Dropdown]                │
│                       - ACCRUAL                 │
│                       - ADJUSTMENT              │
│                       - ALLOCATION              │
│                       - RECLASSIFICATION        │
│ Description         : [Text Field]              │
│ Period (Optional)   : [Month/Year Picker]       │
│                                                  │
│ [SUBMIT] [CANCEL]                              │
└─────────────────────────────────────────────────┘
```

### Step 4.3: Example Journal Entries (Real Scenarios)

#### Example 1: Record Co-op Investments at Year Start

**Scenario:** At the beginning of the year, you receive Co-op Holdings certificate valued at KES 500,000.

```
GL Account:        CO_OP_HOLDINGS
Entry Date:        2026-01-01
Amount:            500,000.00
Is Debit:          YES (because CO_OP_HOLDINGS is an ASSET)
Entry Reason:      ADJUSTMENT (one-time adjustment at year-start)
Description:       "Initial Co-op Holdings investment received - Certificate #2026001"
Period:            January 2026 (optional)

→ SYSTEM STORES: 
  Entry ID: 42
  Status: DRAFT (waiting for treasurer to submit)
```

#### Example 2: Record Audit Fees Expense

**Scenario:** You've engaged an auditor for KES 25,000 but haven't paid yet. You want to record the liability.

```
GL Account:        AUDITOR_PAYABLE (Liability)
Entry Date:        2026-01-10
Amount:            25,000.00
Is Debit:          NO (because AUDITOR_PAYABLE is a LIABILITY, use CREDIT)
Entry Reason:      ACCRUAL (you owe it but haven't paid)
Description:       "Auditor fee for FY2025 - Invoice #AUD-001"
Period:            January 2026

→ SYSTEM STORES:
  Increases AUDITOR_PAYABLE (liability) by 25,000
```

#### Example 3: Record Interest Paid on Member Deposits

**Scenario:** In December, you paid KES 8,500 interest to member savings accounts.

```
GL Account:        INT_DEPOSITS (Revenue)
Entry Date:        2026-12-31
Amount:            8,500.00
Is Debit:          NO (because INT_DEPOSITS is REVENUE, use CREDIT for income)
Entry Reason:      ACCRUAL (interest accrued through the month)
Description:       "Interest accrued on member deposits for December 2025"
Period:            December 2025

→ SYSTEM STORES:
  Increases INT_DEPOSITS revenue by 8,500
```

#### Example 4: Record Processing Fees Collected

**Scenario:** You collected KES 3,200 in loan processing fees during the month.

```
GL Account:        LOAN_PROCESSING_FEE (Revenue)
Entry Date:        2026-01-31
Amount:            3,200.00
Is Debit:          NO (REVENUE = CREDIT)
Entry Reason:      ACCRUAL
Description:       "Loan processing fees collected - January 2026"
Period:            January 2026

→ Result:
  LOAN_PROCESSING_FEE balance increases by 3,200 (appears on Income Statement)
```

#### Example 5: Record SASRA Regulatory Fees

**Scenario:** You received SASRA invoice for KES 15,000 annual regulatory fees.

```
GL Account:        SASRA_FEES (Expense)
Entry Date:        2026-01-15
Amount:            15,000.00
Is Debit:          YES (EXPENSE = DEBIT)
Entry Reason:      ACCRUAL (owed but not paid yet)
Description:       "SASRA annual regulatory fee for 2026"
Period:            January 2026

→ Result:
  SASRA_FEES balance increases by 15,000 (appears on Income Statement as expense)
```

#### Example 6: Record Statutory Reserve Allocation

**Scenario:** End of year: You need to allocate 10% of profits (KES 40,000) to statutory reserve.

```
GL Account:        STATUTORY_RESERVE (Equity)
Entry Date:        2026-12-31
Amount:            40,000.00
Is Debit:          YES (EQUITY debit)
Entry Reason:      ALLOCATION (quarterly/annual allocation)
Description:       "Allocation of 10% of annual profit to statutory reserve per SACCO regulation"
Period:            December 2026

→ Result:
  Increases STATUTORY_RESERVE on Balance Sheet
```

#### Example 7: Loan Loss Provision

**Scenario:** During review, you identify that KES 50,000 in loans are at risk. Record provision.

```
GL Account:        LOAN_LOSS_PROVISION (Expense)
Entry Date:        2026-03-31
Amount:            50,000.00
Is Debit:          YES (EXPENSE debit)
Entry Reason:      ADJUSTMENT (quarterly review adjustment)
Description:       "Loan loss provision for potentially non-performing loans (3 months arrears)"
Period:            March 2026

→ Result:
  LOAN_LOSS_PROVISION increases (appears on Income Statement, reduces net income)
```

### Step 4.4: Entry Status Flow (What Happens After You Create)

After creating an entry:

```
CREATE ENTRY by Treasurer
      ↓
Status = DRAFT (only treasurer can see, can edit/delete)
      ↓
Treasurer clicks "SUBMIT"
      ↓
Status = POSTED (read-only for treasurer, visible to admin)
      ↓
Admin sees it in "Pending Approval" tab
      ↓
Admin reviews and clicks "APPROVE ✓"
      ↓
Status = APPROVED (locked, system now includes in GL calculations)
      ↓
Reports auto-generated with this entry
      ↓
[OR Admin clicks "REJECT ✗" → Status = REJECTED, back to DRAFT for treasurer to fix]
```

---

## PHASE 5: Admin Approval (The Admin's Job)

Once treasurer submits entries, admin must approve them before they affect reports.

### Step 5.1: Frontend - Review Pending Entries

**As Admin, here's what you do:**

1. **Navigate to GL Manual Entries Page** 
   - URL: `/gl-manual-entries`

2. **Click "Pending Approval" Tab**
   - Shows only entries with status = POSTED (submitted by treasurer)

3. **Review Each Entry**
   - GL Account
   - Amount
   - Date
   - Description/Reason
   - Treasurer who created it

4. **Make Decision**
   - ✅ Click **Green Checkmark** to APPROVE
     - Status → APPROVED
     - Entry now included in GL calculations
     - Appears in reports
   
   - ❌ Click **Red X** to REJECT
     - Status → REJECTED
     - Reason required
     - Entry sent back to DRAFT for treasurer to revise
     - Treasurer notified

### Step 5.2: Approval Audit Trail

Every approval/rejection is logged:
```
Timestamp: 2026-01-15 14:30:45
Admin: Sarah Admin
Entry ID: 42
GL Account: CO_OP_HOLDINGS
Amount: 500,000.00
Action: APPROVED
IP Address: 192.168.1.105
Browser: Chrome

Status: SUCCESS (entry now in system)
```

---

## PHASE 6: System Calculates GL Balances

Once entries are APPROVED, the system automatically:

1. **Collects all APPROVED entries** for that GL account
2. **Sums debits** and **credits** separately
3. **Calculates balance** = Sum(Debits) - Sum(Credits)

### Example Calculation

**GL Account: CO_OP_HOLDINGS**

```
APPROVED Journal Entries:
─────────────────────────────────
Date        Amount      Debit/Credit
─────────────────────────────────
2026-01-01  500,000     DEBIT
2026-03-15  (50,000)    CREDIT (sold some)
2026-06-30  100,000     DEBIT (additional purchase)

Calculation:
  Total Debits:   500,000 + 100,000 = 600,000
  Total Credits:  50,000
  Balance:        600,000 - 50,000 = 550,000

Result: GL_BALANCE(CO_OP_HOLDINGS) = 550,000.00
```

---

## PHASE 7: Reports Auto-Generate with Your Data

Once GL balances are calculated, reports automatically pull from them.

### How Balance Sheet Gets Built

```
Balance Sheet Query Process:
────────────────────────────

1. Get all ASSET accounts (15 accounts)
   - For each: Calculate balance from approved entries + auto-calculated sources
   - LOAN_NORMAL: Auto-calculated from loans table
   - LOAN_EMERGENCY_1: Auto-calculated from loans table
   - ...
   - CO_OP_HOLDINGS: Sum of your approved entries = 550,000
   - CBA_CALL_DEPOSITS: Sum of your approved entries = 200,000
   Total Assets: 950,000

2. Get all LIABILITY accounts (6 accounts)
   - For each: Calculate balance
   - MEMBER_DEPOSITS: Auto from accounts table = 1,000,000
   - MEMBER_SHARES: Auto from accounts table = 150,000
   - AUDITOR_PAYABLE: Sum of your entries = 25,000
   Total Liabilities: 1,175,000

3. Get all EQUITY accounts (3 accounts)
   - STATUTORY_RESERVE: Your entries = 40,000
   - REVENUE_RESERVE: Your entries = 20,000
   - RETAINED_EARNINGS: Computed = (285,000)
   Total Equity: (225,000)

4. Balance Check:
   Assets (950,000) = Liabilities (1,175,000) + Equity (-225,000)
   950,000 = 950,000 ✓ BALANCED
```

### How Income Statement Gets Built

```
Income Statement Query Process (for January 2026):
──────────────────────────────────────────────────

1. Get all REVENUE accounts (4 accounts)
   - INT_LOANS: Auto-calculated = 50,000
   - INT_DEPOSITS: Your entries = 8,500
   - ENTRANCE_FEES: Auto-calculated = 5,000
   - LOAN_PROCESSING_FEE: Your entries = 3,200
   Total Revenues: 66,700

2. Get all EXPENSE accounts (11 accounts)
   - AUDIT_FEES: Your entries = 10,000
   - TRAVEL_EXPENSES: Your entries = 3,000
   - SASRA_FEES: Your entries = 15,000
   - ...
   Total Expenses: 35,500

3. Calculate Net Income:
   Net Income = 66,700 - 35,500 = 31,200
```

---

## PHASE 8: Generate and Review Reports

### Step 8.1: Frontend - Access Reports

1. **Navigate to Reports Page**
   - URL: `/reports`

2. **Select Report Type**
   - Balance Sheet
   - Income Statement
   - Trial Balance

3. **Specify Parameters**
   - Date range or "as of" date
   - Period (month/year) if needed

4. **View Report**
   - All calculations automatic
   - All data from your approved GL entries

---

## The Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│          OPERATIONAL TRANSACTIONS                            │
│  (Loans, Deposits, Withdrawals, Fees, Interest, etc.)      │
└──────────────────────┬──────────────────────────────────────┘
                       │ (Auto-aggregated)
                       ↓
        ┌─────────────────────────────┐
        │ AUTO-CALCULATED GL ACCOUNTS │
        │ (no manual entries needed)   │
        │ - LOAN_NORMAL               │
        │ - MEMBER_DEPOSITS           │
        │ - INT_LOANS                 │
        │ - ENTRANCE_FEES             │
        │ - BANK_CHARGES              │
        └──────────────┬──────────────┘
                       │
                       ↓
    ┌──────────────────────────────────────┐
    │   TREASURY CREATES JOURNAL ENTRIES    │
    │  (For MANUAL_ENTRY GL accounts)      │
    │  - CO_OP_HOLDINGS                    │
    │  - AUDIT_FEES                        │
    │  - INTEREST_PAYABLE                  │
    │  - LOAN_PROCESSING_FEE               │
    │  - ... (28 more accounts)            │
    └────────────────┬─────────────────────┘
                     │ (SUBMIT)
                     ↓
       ┌─────────────────────────────┐
       │ ENTRY STATUS = POSTED        │
       │ (Awaiting Admin Approval)    │
       └──────────────┬──────────────┘
                      │
                      ↓
       ┌─────────────────────────────┐
       │  ADMIN REVIEWS & APPROVES    │
       │  - Approve ✓ or Reject ✗    │
       │  (All actions audited)       │
       └──────────────┬──────────────┘
                      │
                      ↓
       ┌─────────────────────────────┐
       │ ENTRY STATUS = APPROVED      │
       │ (Ready for GL calculations)  │
       └──────────────┬──────────────┘
                      │
           ┌──────────┴──────────┐
           ↓                     ↓
    ┌─────────────┐      ┌─────────────┐
    │ MANUAL      │      │ AUTO-CALC   │
    │ ENTRIES     │      │ ACCOUNTS    │
    │ (Approved)  │      │ (Always     │
    │             │      │  current)   │
    └──────┬──────┘      └──────┬──────┘
           │                    │
           └────────┬───────────┘
                    ↓
         ┌──────────────────────┐
         │  GL CALCULATIONS     │
         │  Account Balance =   │
         │  Σ(Debits) - Σ(Credits)
         └──────────┬───────────┘
                    │
        ┌───────────┴───────────┐
        ↓                       ↓
   ┌─────────┐          ┌──────────────┐
   │ Balance │          │ Income       │
   │ Sheet   │          │ Statement    │
   │ (Assets │          │ (Revenues -  │
   │ = Liab  │          │  Expenses)   │
   │ + Equi) │          │              │
   └─────────┘          └──────────────┘
        │                      │
        └──────────┬───────────┘
                   ↓
           ┌──────────────────┐
           │ EXPORTED REPORTS │
           │ (PDF, Excel, etc)│
           │ for stakeholders │
           └──────────────────┘
```

---

## Summary: Step-by-Step Checklist

### Week 1: Setup
- [ ] Verify all GL accounts are configured and ACTIVE
- [ ] Understand which accounts are AUTOMATIC vs MANUAL
- [ ] Train treasury staff on which accounts need entries
- [ ] Train admin staff on approval workflow

### Week 2-4: Data Entry (Monthly Cycle)
- [ ] **Day 1-20:** Treasurer creates journal entries for manual accounts
- [ ] **Day 21:** Treasurer submits all entries (POSTED status)
- [ ] **Day 22-25:** Admin reviews and approves entries
- [ ] **Day 26:** System calculates GL balances automatically
- [ ] **Day 27-30:** Reports generated and reviewed

### Ongoing: Monthly Cycle
```
Each Month:
  1. Treasurer enters journal entries (CO_OP_HOLDINGS, AUDIT_FEES, etc.)
  2. Treasurer submits entries (status = POSTED)
  3. Admin approves entries (status = APPROVED)
  4. GL system calculates balances automatically
  5. Reports generated automatically
  6. Finance team exports and reviews reports
  7. Repeat next month
```

---

## Common Questions

### Q: What if I enter the same amount in multiple periods?
**A:** You can. Just specify period (month/year) when creating entry. System will calculate per-period balances.

### Q: Can I edit an entry after submitting?
**A:** No. Once POSTED, it's read-only. If rejected, it goes back to DRAFT (only then editable).

### Q: What if balance sheet doesn't balance?
**A:** Check:
1. Are all MANUAL entries approved?
2. Are all account calculations correct?
3. Run Trial Balance to find discrepancies

### Q: How do I reverse an entry?
**A:** Create an opposite entry:
- Original: Debit CO_OP_HOLDINGS 500,000
- Reversal: Credit CO_OP_HOLDINGS 500,000
- Both must be approved

### Q: Can I enter GL entries retroactively?
**A:** Yes. Use the entry date field to date it whenever needed. System will include it in historical reports.

---

## What You Now Have

✅ **29 GL Accounts** pre-configured and categorized
✅ **Automatic calculations** for operational data (loans, deposits, fees)
✅ **Manual entry system** for accounting adjustments
✅ **Admin approval workflow** with audit trail
✅ **Balance Sheet** auto-generated from GL accounts
✅ **Income Statement** auto-generated from GL accounts
✅ **Trial Balance** for reconciliation
✅ **All reports** updated when entries approved

**Your SACCO's GL system is production-ready.**
