# TRIAL BALANCE DATA MAPPING ANALYSIS
## Minet SACCO System - Current State vs Standard Trial Balance

**Prepared for:** Leadership Meeting  
**Date:** June 2, 2026  
**Purpose:** Assessment of what data the system captures and how it maps to a professional trial balance

---

## EXECUTIVE SUMMARY

Your system **captures member-level transaction data** but is currently structured for **individual member statements**, not **consolidated organizational accounts**. A proper trial balance requires **chart of accounts** with GL account codes and double-entry bookkeeping principles.

**Current vs Required:**
- ✅ System tracks: Member deposits, loans, repayments, transactions
- ❌ Missing: Organizational GL accounts, accruals, payables, reserves structure
- ❌ Missing: Financial asset accounts, bank reconciliations, expense categories

---

## WHAT YOUR SYSTEM CURRENTLY STORES

### 1. **MEMBER ACCOUNTS** (Customer Liability Accounts)
```
Table: accounts
Columns: member_id, account_type, balance, frozen_savings

Account Types:
├── SAVINGS (Member deposits - Core liability account)
├── SHARES (Share capital - Equity component)
├── CONTRIBUTIONS (Monthly mandatory contributions)
├── BENEVOLENT_FUND (Welfare fund)
├── DEVELOPMENT_FUND (Project fund)
├── SCHOOL_FEES (Education savings)
├── HOLIDAY_FUND (Seasonal savings)
└── EMERGENCY_FUND (Personal emergency savings)

Current Report: Trial Balance shows MEMBER DEPOSITS as single "Cr." line
Actual Structure: 8 different account types per member aggregated as liabilities
```

**Mapping to Your Trial Balance:**
- Line: "Member deposits: KES 165,401,021 (Cr.)" 
- System Source: SUM(accounts.balance) WHERE account_type IN (SAVINGS, SHARES, etc.)
- **Issue:** No breakdown by account type; no reserve classifications

---

### 2. **LOAN ACCOUNTS** (Asset Accounts)
```
Table: loans, loan_repayments
Captures: amount, interest_rate, term_months, total_repayable, outstanding_balance

Loan Types:
├── Normal loan (tracked by loan_product.name)
├── Emergency loan 1
├── Emergency loan 2
└── (Other products via LoanProduct table)

Status Tracking: PENDING → APPROVED → DISBURSED → REPAID/DEFAULTED
Interest Calculation: Simple interest = Principal × Rate × Time
```

**Mapping to Your Trial Balance:**
```
Line: "Normal loan: Dr. 99,629,963 | Cr. 98,816,508"
Line: "Emergency loan 1: Cr. 998,147"

System Captures:
- DR side: Sum of disbursed loan amounts (assets)
- CR side: Sum of repayments received (liability reversal)
- Missing: Loan loss reserve calculations, impairment provisions
```

**Critical Gap:** Your system stores `outstandingBalance` (after repayments) but trial balance shows:
- Total Issued (Dr.)
- Total Repaid (Cr.)  
- Net Outstanding = Dr. - Cr.

---

### 3. **CASH & BANK ACCOUNTS**
```
Table: NO DEDICATED BANK ACCOUNT TABLE
Current Limitation: Only member savings accounts tracked

Missing:
- CBA Call deposits (Bank account - asset)
- CBA Current account (Bank account - asset)
- Bank transaction reconciliation
- Bank charges tracking

Workaround: Could be tracked in FundConfiguration, but not standard
```

**Mapping to Your Trial Balance:**
```
Lines showing:
- "CBA Call deposits: KES 48,802,932"
- "CBA Current account: KES 44,018,431"

Current System Status: ❌ NOT CAPTURED in core entities
Possible Storage: In SystemSettings or manual entry
```

---

### 4. **TRANSACTIONS** (Transaction Journal)
```
Table: transactions
Columns: account_id, transaction_type, amount, description, transaction_date

Transaction Types:
├── DEPOSIT (Member deposit)
├── WITHDRAWAL (Member withdrawal)
├── LOAN_DISBURSEMENT (Loan issued)
├── LOAN_REPAYMENT (Loan payment received)
├── INTEREST (Interest earned)
└── LOAN_DEFAULT_DEBIT (Default write-off)

Per-Member Level Tracking: ✅ Yes
Organizational Consolidation: ❌ Not built-in
```

**Mapping to Trial Balance:**

Your TB shows organizational REVENUE & EXPENSES:
```
Revenue Lines:
- Interest - loans: KES 12,265,794
- Interest on call deposits - bank: KES 7,342,493
- Entrance fee: KES 20,500

Expense Lines:
- Administrative (Audit, Travel, Training): KES 202,750
- Governance (Committee allowances, AGM): KES 438,000
- Other (Insurance, Bank charges, Impairment): KES 16,379,857

Current System Status: ❌ NOT TRACKED in transactions table
Missing: Revenue recognition, expense categories, journal entries
```

---

### 5. **GUARANTOR & GUARANTEES**
```
Table: guarantor (tracks pledge amounts, approval status)
Currently Captures: Who guarantees whom, pledge amounts

Missing in Trial Balance Line Items: No explicit accounting
Issue: Contingent liabilities not disclosed
```

---

## GAP ANALYSIS: YOUR TB vs WHAT SYSTEM CAPTURES

### ✅ WHAT SYSTEM CAN GENERATE TODAY

1. **Member Deposits Liability** (165M+)
   - From: `SELECT SUM(balance) FROM accounts`
   - Accuracy: ✅ Good

2. **Loans Outstanding (Assets)**
   - From: `SELECT SUM(outstanding_balance) FROM loans WHERE status='DISBURSED'`
   - Accuracy: ⚠️ Fair (depends on correct repayment recording - see earlier fixes)

3. **Loan Repayment History**
   - From: `SELECT SUM(amount) FROM loan_repayments`
   - Accuracy: ✅ Good

4. **Member Transactions (Deposits/Withdrawals)**
   - From: `SELECT SUM(amount) FROM transactions WHERE transaction_type='DEPOSIT'`
   - Accuracy: ✅ Good

---

### ❌ WHAT SYSTEM CANNOT GENERATE YET

Your trial balance includes these GL accounts **not tracked in system:**

| GL Account | TB Amount | System Status | Data Source |
|-----------|-----------|--------------|------------|
| Co-opholdings Co-operative Society Ltd | 21,300 | ❌ Missing | Manual/External |
| Cooperative Insurance Company Limited | 6,772 | ❌ Missing | Manual/External |
| KUSCCO Limited | 11,665 | ❌ Missing | Manual/External |
| CBA Call deposits | 48,802,932 | ❌ Missing | Manual/External |
| CBA Current account | 44,018,431 | ❌ Missing | Manual/External |
| Sacco management System (Receivable) | 496,664 | ❌ Missing | Manual/External |
| Loan Loss reserve | 998,147-1,546,180 | ⚠️ Partial | Can calculate but not stored |
| Interest on Members Deposits (Payable) | 15,713,097 | ❌ Missing | Accrual/calculated |
| Proposed Dividends | 80,340 | ❌ Missing | Accrual/proposed |
| Proposed Honoraria | 150,000 | ❌ Missing | Accrual/proposed |
| Committee sitting allowance | 98,000 | ❌ Missing | Accrual/payable |
| Auditors' remuneration | 90,000 | ❌ Missing | Accrual/payable |
| AGM expenses | 50,000 | ❌ Missing | Accrual/payable |
| Revenue Reserve | 5,971,500 | ❌ Missing | Reserve account |
| Statutory reserve | 2,456,133 | ❌ Missing | Reserve account |
| Share capital | 387,660-618,000 | ❌ Missing | Member equity |
| Audit fees (expense) | 90,000 | ❌ Missing | Expense recognition |
| All insurance/bank charges/taxes | Various | ❌ Missing | Expense tracking |

---

## DETAILED ACCOUNT MAPPING

### BALANCE SHEET ACCOUNTS (Statement of Financial Position)

#### **ASSETS**
```
Current Assets:
├── Cash & Cash Equivalents
│   ├── CBA Call deposits: [NOT TRACKED] KES 48,802,932
│   ├── CBA Current account: [NOT TRACKED] KES 44,018,431
│   └── Total: KES 92,821,363
│
├── Receivables
│   ├── Loans Outstanding: [TRACKED] 
│   │   ├── Normal loan net: 99,629,963 - 98,816,508 = KES 813,455
│   │   ├── Emergency loan 1: -998,147 (over-repaid)
│   │   ├── Emergency loan 2: KES 9,907
│   │   └── Total Loan Assets: KES (174,785) [NET - currently in error state]
│   │
│   ├── Sacco Management System [NOT TRACKED]: KES 496,664
│   └── Co-operative Investments [NOT TRACKED]: KES 39,737
│
└── Non-Current Assets
    └── Investment Shares [NOT TRACKED]: KES 39,737
```

**System Capability:**
- ✅ Loan assets tracked per loan
- ❌ Bank accounts not tracked
- ❌ Other receivables not tracked
- ❌ Investment accounts not tracked

---

#### **LIABILITIES**
```
Current Liabilities:
├── Member Deposits: [TRACKED] 
│   └── Sum(accounts.balance): KES 165,401,021
│
├── Accrued Interest on Deposits [NOT TRACKED]: KES 15,713,097
│   Status: Calculated but NOT stored as payable liability
│   Issue: System doesn't track accrual entries
│
├── Auditors' remuneration payable [NOT TRACKED]: KES 90,000
├── AGM expenses payable [NOT TRACKED]: KES 50,000  
├── Committee allowances payable [NOT TRACKED]: KES 98,000
└── Proposed dividend [NOT TRACKED]: KES 80,340
```

**System Capability:**
- ✅ Member deposits accurately tracked
- ❌ Accrued expenses not tracked
- ❌ Payables not tracked
- ❌ No GL account structure for liabilities

---

#### **EQUITY**
```
Share Capital [NOT TRACKED]: KES 387,660 - KES 618,000 (variance)
Statutory Reserve [NOT TRACKED]: KES 2,456,133
Revenue Reserve [NOT TRACKED]: KES 5,971,500 ± 230,340
Retained Earnings [NOT TRACKED]: Varies
```

**System Capability:**
- ❌ No equity tracking
- ❌ No reserve management
- ❌ No dividend/distribution tracking
- ⚠️ Share capital could be derived from member.shareCapitalAmount if tracked

---

### INCOME STATEMENT ACCOUNTS (Statement of Comprehensive Income)

#### **REVENUE**
```
Interest Income:
├── Interest - loans [PARTIALLY TRACKED]
│   System: Can calculate from Loan.interestRate × outstanding_balance
│   Current storage: Only in Transaction table if manually entered
│   TB shows: KES 12,265,794 (❌ Not system-generated)
│
├── Interest on call deposits [NOT TRACKED]: KES 7,342,493
└── Entrance fee [TRACKED]
    System: Transaction type = ENTRANCE_FEE, Transaction type doesn't exist yet
    TB shows: KES 20,500
```

**System Capability:**
- ❌ No interest accrual system
- ❌ No revenue recognition rules
- ❌ No deposit interest calculation
- ❌ Entrance fees not specifically tracked (would be in member creation)

---

#### **EXPENSES**
```
Administrative Expenses [NOT TRACKED]:
├── Audit fees: KES 90,000
├── Travelling: KES 20,000
├── SASRA annual fees: KES 30,000
└── Training and education: KES 62,750
Total: KES 202,750

Governance Expenses [NOT TRACKED]:
├── Committee sitting allowances: KES 338,000
├── AGM expenses: KES 50,000
└── Total: KES 388,000

Other Operating Expenses [NOT TRACKED]:
├── Insurance premiums: KES 528,645
├── Bank charges: KES 38,623
├── Impairment provision: KES 110,118
└── Interest expense (on member deposits): KES 15,713,097
Total: KES 16,390,483

Income Tax [NOT TRACKED]: KES 1,101,374
```

**System Capability:**
- ❌ No expense recognition
- ❌ No GL expense accounts
- ❌ No depreciation tracking
- ❌ No provision calculations (loan loss, impairment)
- ❌ No tax tracking

---

## CURRENT REPORTS CAPABILITY

### ✅ What ReportsService Currently Generates

1. **Cashbook Report** (Line 35)
   - Shows transaction-by-transaction cash movements
   - Totals: Deposits, Withdrawals, Repayments, Net Cash
   - Source: Transactions table
   - Accuracy: ✅ Good (operational level)

2. **Trial Balance Report** (Line 97)
   - Current Implementation:
     ```
     For each member account:
       DR = 0, CR = balance (member liability)
     For each disbursed loan:
       DR = outstanding_balance, CR = 0
     Calculate: Total DR must equal Total CR
     ```
   - Current Issue: ⚠️ **SHOWS ONLY MEMBER-LEVEL DETAIL**
   - Problem: Not a consolidated organizational trial balance
   - Missing: GL accounts, expense accounts, revenue accounts

3. **Balance Sheet Report** (Line 157)
   - Current Implementation: Aggregates assets vs liabilities vs equity
   - What it shows: Total of all member accounts as one liability line
   - Missing: Detailed asset, liability, equity breakdowns per your TB

4. **Member Statement** (Line 200)
   - Shows individual member transactions
   - Accuracy: ✅ Good (member-level)

5. **Loan Register** (Line 267)
   - Shows all loans with details
   - Includes: Principal, Interest Rate, Term, Monthly Payment, Outstanding
   - Accuracy: ⚠️ Fair (depends on outstanding_balance calculation - fixed earlier)

---

## WHAT NEEDS TO BE CAPTURED

### Phase 1: Core GL Accounting Structure (Prerequisite for TB)
```
Table: chart_of_accounts
├── account_code (e.g., "1010", "2050", "3100")
├── account_name (e.g., "CBA Call Deposits", "Member Deposits", "Interest Income")
├── account_type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)
├── is_active (boolean)
└── balance (can be calculated or stored)

This table would list ALL accounts on your trial balance
```

### Phase 2: GL Journal Entries (Double-Entry Bookkeeping)
```
Table: journal_entries
├── entry_date
├── description
├── reference (e.g., "DEP-2026-001")
└── line_items (related table: journal_entry_lines)

Each transaction posts to 2+ GL accounts:
  DEBIT: CBA Current Account
  CREDIT: Member Deposits
  Amount: 50,000
  Description: Member deposit received
```

### Phase 3: Financial Statement Mappings
```
Which GL accounts roll up to:
- Balance Sheet (Assets, Liabilities, Equity)
- Income Statement (Revenue, Expenses)
- Trial Balance (Debit/Credit totals)
```

---

## RECOMMENDATIONS FOR YOUR MEETING

### **Priority 1: Urgent Data Capture Gaps** 
These **prevent trial balance generation**:

1. **Bank Accounts**
   - Add table: `bank_accounts` (account_number, bank_name, balance, last_reconciliation)
   - Required for: CBA Call deposits, CBA Current account lines

2. **GL Chart of Accounts**
   - Create standard SACCO chart per SASRA guidelines
   - Map all accounts from your TB to system accounts

3. **Expense Tracking**
   - Add expense categories to system
   - Track: Audits, allowances, insurance, training, bank fees

4. **Accrued Liabilities**
   - Interest payable on member deposits
   - Proposed honoraria/dividends
   - SASRA fee accruals

### **Priority 2: Medium-term Structure** (Quarterly reporting)
5. Reserve management (Statutory reserve movements)
6. Loan loss impairment calculations
7. Member equity tracking (share capital + retained earnings)

### **Priority 3: Long-term (Annual audits)**
8. Tax expense tracking
9. Depreciation on assets
10. Investment account tracking

---

## HONEST ASSESSMENT

| Area | Current Capability | Target State | Gap |
|------|------------------|--------------|-----|
| Member Accounts | ✅ Tracked accurately | Accurate TB | Small |
| Loan Assets | ⚠️ Tracked (with calculation issues we fixed) | Accurate TB | Medium |
| Bank/Cash | ❌ Not tracked | Required for TB | Large |
| Revenue Recognition | ❌ Not tracked | Interest revenue line | Large |
| Expense Tracking | ❌ Not tracked | Full expense section | Large |
| Accruals | ❌ Not tracked | Interest payable, payables | Large |
| Reserves/Equity | ❌ Not tracked | Full equity section | Large |
| **Overall TB Coverage** | **~30%** | **100%** | **Large** |

---

## ANSWER TO YOUR QUESTION

**Q: "What kind of data does the system store and can give a report?"**

**A:**
- The system stores **operational transaction data** (member deposits, loans, repayments)
- It can generate **member-level reports** (statements, cashbook)
- It **cannot currently generate** a professional trial balance per accounting standards
- To generate your TB, we need to add **GL accounting structure** (chart of accounts, journal entries)

**What you should tell your meeting:**
> "Our current system is 30% complete for financial reporting. We have solid member account tracking, but we're missing the GL accounting structure needed for consolidated financial statements. With chart of accounts and journal entry tracking, we can generate the trial balance. This is a **structural enhancement, not a fix** – we need to add accounting layer on top of operational data."

---

## NEXT STEPS AFTER MEETING

1. **Get approval** to add GL accounting structure
2. **Document** your exact chart of accounts (get from auditors)
3. **Map** your current data to new GL accounts
4. **Implement** journal entry table + posting logic
5. **Re-run** trial balance report against GL (not member accounts)

---

**Note:** As requested, **no changes have been made to the system**. This is analysis only.
