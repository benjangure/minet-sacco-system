# Kenyan SACCO Reporting Roadmap: GL Mapping Foundation

## Vision
Once GL mapping layer is built, **all reports** become GL-based and SACCO-compliant:
- ✅ Trial Balance
- ✅ Balance Sheet
- ✅ Income Statement (P&L)
- ✅ Cashbook
- ✅ Loan Register
- ✅ Member Statements
- ✅ Cash Flow
- ✅ SASRA Compliance Reports

---

## Phase Architecture

```
PHASE 1: GL Mapping Foundation (6-8 hours)
├─ Database: gl_accounts, gl_account_calculations, gl_manual_entries
├─ Service: GLCalculationService (calculation engine)
├─ API: GET /api/reports/trial-balance
└─ UI: Trial Balance report display

        ↓ (Everything below builds on this)

PHASE 2: Financial Statements from GL (4-6 hours)
├─ Balance Sheet (Assets = Liabilities + Equity)
├─ Income Statement (Revenues - Expenses)
├─ Cash Flow Statement
└─ SASRA Compliance Report

PHASE 3: Operational Reports from GL (3-4 hours)
├─ Cashbook (GL CASH account movements)
├─ Loan Register (GL LOAN accounts)
├─ Member Statements (GL member-level detail)
└─ Fund Distribution Report

PHASE 4: Advanced Features (Optional, 4-6 hours)
├─ Budget vs Actual
├─ Comparative Reports (Month/Year-over-Year)
├─ Audit Trail per GL account
└─ Export to SASRA formats
```

---

## Why GL Mapping Enables Everything

### Current State (No GL)
```
Member Loan Balance Report
  → Query loans table directly
  → Shows only loan data
  → No context of how it fits in accounting

Cashbook Report
  → Query transactions directly
  → Shows only transaction data
  → Doesn't link to financial position
```

### With GL Mapping
```
Member Loan Balance Report
  ↓ Points to GL Account
  → GL Account has calculation config
  → Queries loans table using config
  → Context: Part of "Normal Loan" asset on trial balance
  → Can be included in Balance Sheet

Cashbook Report
  ↓ Points to GL Account (CASH)
  → GL CASH account = sum of all bank accounts
  → Can filter by bank (CBA Call, CBA Current, etc.)
  → Context: Shows GL cash movement aligned with trial balance
  ↓ Automatically reconciles to Balance Sheet cash position
```

---

## Phase 1 Implementation: GL Mapping Foundation

### What Gets Created

**1. Database Tables**
```sql
gl_accounts (25-30 accounts from your trial balance)
gl_account_calculations (how each account calculates)
gl_manual_entries (treasurer-entered values)
```

**2. Backend Service: GLCalculationService**
```java
- calculateGLAccountBalance(glAccountId, asOfDate)
- generateTrialBalance(asOfDate)
- getAccountHierarchy() // Groups by type
- validateTrialBalance() // Dr = Cr check
```

**3. API Endpoints**
```
GET /api/gl/accounts                    // List all GL accounts
GET /api/gl/accounts/{id}               // Get GL account detail
POST /api/gl/accounts                   // Create (treasurer/admin)
PUT /api/gl/accounts/{id}               // Update calculation config
GET /api/reports/trial-balance          // Generate trial balance
GET /api/gl/{id}/balance?date=2024-01-31 // Balance as of date
```

**4. Initial GL Account Setup** (From your trial balance)
```
ASSETS:
- LOAN_NORMAL (code: LOAN_NORMAL, calc: SUM loans where type='NORMAL')
- LOAN_EMERGENCY_1 (calc: SUM loans where type='EMERGENCY_1')
- LOAN_EMERGENCY_2 (calc: SUM loans where type='EMERGENCY_2')
- CBA_CALL_DEPOSITS (calc: SUM accounts where bank='CBA_CALL')
- CBA_CURRENT (calc: SUM accounts where bank='CBA_CURRENT')
- RECEIVABLES_SMS (calc: SUM receivables where type='SMS')
- CO_OP_HOLDINGS (manual entry or fixed)
- COOP_INSURANCE (manual entry or fixed)
- KUSCCO (manual entry or fixed)

LIABILITIES:
- MEMBER_DEPOSITS (calc: SUM accounts where type='SAVINGS'... all member accounts)
- AUDITOR_PAYABLE (manual entry)
- DIVIDEND_PROPOSED (manual entry)
- INTEREST_PROPOSED (manual entry: formula or manual)
- COMMITTEE_ALLOWANCE_PAYABLE (manual entry)

EQUITY:
- SHARE_CAPITAL (calc: SUM accounts where type='SHARES')
- STATUTORY_RESERVE (manual entry or formula: % of profit)
- REVENUE_RESERVE (manual entry)

REVENUE:
- INT_LOANS (calc: SUM transactions where type='INTEREST_RECEIVED')
- ENTRANCE_FEE (calc: SUM transactions where type='ENTRANCE_FEE')
- INT_DEPOSITS (manual entry: accrual)

EXPENSES:
- AUDIT_FEES (manual entry)
- TRAVEL (manual entry)
- SASRA_FEES (manual entry)
- TRAINING (manual entry)
- COMMITTEE_ALLOWANCES (manual entry)
- AGM_EXPENSES (manual entry)
- INSURANCE_PREMIUMS (manual entry)
- BANK_CHARGES (calc: SUM transactions where type='BANK_CHARGE')
- IMPAIRMENT_LOANS (manual entry: LLP calculation)
- INTEREST_EXPENSE (manual entry: proposed dividend)
- INCOME_TAX (manual entry)
```

---

## Phase 2: Financial Statements from GL

**Once GL mapping is live, generate all reports:**

### 2.1 Balance Sheet
```
Endpoint: GET /api/reports/balance-sheet?date=2024-01-31

Query all GL accounts, group by type:

ASSETS (Dr balance)
├─ Financial assets
│  ├─ Co-op Holdings: 21,300
│  └─ KUSCCO: 11,665
├─ Loans
│  ├─ Normal loan: 99,629,963
│  ├─ Emergency loan 1: 174,784
│  └─ Emergency loan 2: 9,907
├─ Cash
│  ├─ CBA Call: 48,802,932
│  └─ CBA Current: 44,018,431
└─ Receivables: 496,664
───────────────────────────────
TOTAL ASSETS: 193,165,686

LIABILITIES (Cr balance)
├─ Member deposits: 165,401,021
├─ Payables
│  ├─ Auditors: 90,000
│  ├─ Dividend: 80,340
│  ├─ Interest: 15,713,097
│  └─ Allowances: 98,000
───────────────────────────────
TOTAL LIABILITIES: 181,382,458

EQUITY
├─ Share capital: 618,000
├─ Statutory reserve: 2,456,133
├─ Revenue reserve: 5,971,500
├─ Retained earnings: 2,737,595
───────────────────────────────
TOTAL EQUITY: 11,783,228

Total Liabilities + Equity: 193,165,686 ✓
```

**Implementation**:
```java
@Service
public class FinancialStatementService {
  
  public BalanceSheetDTO generateBalanceSheet(LocalDate asOfDate) {
    List<GLAccount> assets = glAccountRepository.findByType(ASSET);
    List<GLAccount> liabilities = glAccountRepository.findByType(LIABILITY);
    List<GLAccount> equity = glAccountRepository.findByType(EQUITY);
    
    BigDecimal totalAssets = assets.stream()
      .map(a -> glCalcService.calculateBalance(a.getId(), asOfDate))
      .reduce(ZERO, add);
    
    // Similar for liabilities and equity
    
    return new BalanceSheetDTO(asOfDate, assets, liabilities, equity);
  }
}
```

### 2.2 Income Statement
```
Endpoint: GET /api/reports/income-statement?from=2024-01-01&to=2024-01-31

REVENUES
├─ Interest - loans: 12,265,794
├─ Interest - deposits: 7,342,493
├─ Entrance fees: 20,500
────────────────────────────────
TOTAL REVENUES: 19,628,787

EXPENSES
├─ Admin
│  ├─ Audit fees: 90,000
│  ├─ Travelling: 20,000
│  ├─ SASRA fees: 30,000
│  └─ Training: 62,750
├─ Governance
│  ├─ Committee allowances: 338,000
│  └─ AGM expenses: 50,000
├─ Other
│  ├─ Insurance: 528,645
│  ├─ Bank charges: 38,623
│  ├─ Impairment: 110,118
│  ├─ Interest expense: 15,713,097
│  └─ Tax: 1,101,374
────────────────────────────────
TOTAL EXPENSES: 18,082,607

NET INCOME: 1,546,180
```

### 2.3 Cash Flow Statement
```
Endpoint: GET /api/reports/cash-flow?from=2024-01-01&to=2024-01-31

OPERATING ACTIVITIES
├─ Loans disbursed: (X)
├─ Loan repayments: Y
├─ Member deposits: Z
├─ Member withdrawals: (W)
────────────────────────────────
Net cash from operations

INVESTING ACTIVITIES
├─ Co-op investments: (amount)
├─ Equipment purchases: (amount)
────────────────────────────────
Net cash from investing

FINANCING ACTIVITIES
├─ Share capital received: amount
├─ Dividend paid: (amount)
────────────────────────────────
Net cash from financing

Net change in cash position
```

---

## Phase 3: Operational Reports from GL

### 3.1 Cashbook (GL CASH Account Breakdown)

**Current Cashbook Problem**: Only shows transaction-level detail

**With GL Mapping**:
```
Endpoint: GET /api/reports/cashbook?account=CASH&from=2024-01-01&to=2024-01-31

GL Account: CASH (aggregate of all bank accounts)
GL Balance: 92,821,363 (CBA Call + CBA Current)

Transactions (reconciled to GL balance):
┌─ Date ─┬─ Ref ─┬─ Description ───────┬─ Receipts ┬─ Payments ┬─ Balance ──┐
│ 2024-01│ LN001 │ Loan disbursed       │ 0         │ 500,000   │ 92,321,363│
│ 2024-01│ RP001 │ Loan repayment       │ 250,000   │ 0         │ 92,571,363│
│ 2024-01│ DEP01 │ Member deposit       │ 1,000,000 │ 0         │ 93,571,363│
│ 2024-01│ FEE01 │ Bank charges         │ 0         │ 5,000     │ 93,566,363│
└─────────┴───────┴─────────────────────┴───────────┴───────────┴───────────┘

Reconciliation:
GL CASH balance: 92,821,363 ✓
```

**Implementation**:
```java
public CashbookDTO getCashbook(LocalDate from, LocalDate to) {
  // Get GL CASH account
  GLAccount cashAccount = glAccountRepository.findByCode("CASH");
  
  // Get all transactions affecting CASH GL account
  List<Transaction> transactions = transactionRepository
    .findByDateBetween(from, to)
    .stream()
    .filter(t -> mapsToGLAccount(t, cashAccount))
    .collect(toList());
  
  // Calculate running balance
  BigDecimal balance = calculateOpeningBalance(cashAccount, from);
  transactions.forEach(t -> balance = balance.add(t.getAmount()));
  
  return new CashbookDTO(cashAccount, transactions, balance);
}
```

### 3.2 Loan Register

**Currently**: Shows all loans with status

**With GL Mapping**:
```
Endpoint: GET /api/reports/loan-register?type=NORMAL

Loan Register: Normal Loans
GL Account: LOAN_NORMAL
GL Balance: 99,629,963

Loans Detail:
┌─ Member ──┬─ Loan ID ─┬─ Amount ─┬─ Outstanding ┬─ Status ──┬─ Arrears ─┐
│ M001      │ LN001     │ 500,000  │ 250,000      │ ACTIVE    │ 0         │
│ M002      │ LN002     │ 1,000,000│ 1,000,000    │ ACTIVE    │ 0         │
│ M003      │ LN003     │ 750,000  │ 0            │ REPAID    │ 0         │
└───────────┴───────────┴──────────┴──────────────┴───────────┴───────────┘

Total disbursed: 2,250,000
Total outstanding: 1,250,000
GL Balance: 99,629,963 (includes other loans + historical)
```

### 3.3 Member Statement

**Currently**: Shows member account balances

**With GL Mapping**:
```
Endpoint: GET /api/reports/member-statement/{memberId}

Member: John Doe (M001)

SAVINGS (GL: MEMBER_DEPOSITS - SAVINGS subset)
├─ Savings account: 50,000
├─ Shares account: 25,000
├─ Contributions: 10,000
├─ Benevolent fund: 5,000
├─ School fees fund: 2,000
└─ Emergency fund: 3,000
─────────────────────────────
Total savings/deposits: 95,000

LOANS (GL: LOAN_NORMAL, LOAN_EMERGENCY, etc - member subset)
├─ Loan 1: Borrowed 500,000, Outstanding 250,000
├─ Loan 2: Borrowed 750,000, Outstanding 0 (Repaid)
─────────────────────────────
Total borrowed: 1,250,000
Total outstanding: 250,000

GUARANTOR OBLIGATIONS (GL: GUARANTEE_PAYABLE - if tracked)
├─ Guarantor for member M005: Pledge 100,000
```

---

## Phase 4: SASRA & Advanced Compliance

### 4.1 SASRA Compliance Report
```
Endpoint: GET /api/reports/sasra-compliance?period=2024-Q1

SACCO Statistics:
├─ Total members: 500
├─ New members: 45
├─ Dormant members: 12
├─ Suspended members: 3

Financial Position:
├─ Total assets (GL): 193,165,686
├─ Total liabilities (GL): 181,382,458
├─ Net worth (GL equity): 11,783,228
├─ Capital adequacy ratio: 6.1%

Portfolio:
├─ Total loans outstanding: (GL LOAN_* sum)
├─ PAR > 30 days: Amount
├─ Loan loss reserve: (GL)
├─ Reserve adequacy: %

Compliance:
├─ Statutory reserve: (GL)
├─ Statutory reserve minimum: 10% of net assets
├─ Status: ✓ Compliant
├─ Capital minimum: 1,000,000
├─ Status: ✓ Compliant
```

### 4.2 Budget vs Actual
```
Endpoint: GET /api/reports/budget-vs-actual?period=2024

Once budget GL accounts created:

Revenue Budget vs Actual:
├─ Interest - loans
│  ├─ Budget: 12,000,000
│  ├─ Actual: 12,265,794
│  └─ Variance: +2.2% ✓
├─ Entrance fees
│  ├─ Budget: 30,000
│  ├─ Actual: 20,500
│  └─ Variance: -31.7% ⚠
```

---

## Implementation Timeline

| Phase | Tasks | Time | Dependencies |
|-------|-------|------|--------------|
| 1 | GL tables, GLCalcService, Trial Balance | 6-8h | None |
| 2 | Balance Sheet, P&L, Cash Flow | 4-6h | Phase 1 |
| 3 | Cashbook, Loan Register, Member Stmt | 3-4h | Phase 1 |
| 4 | SASRA, Budget, Audit | 4-6h | Phases 1-3 |
| **Total** | | **17-24h** | |

---

## What Makes This "Kenyan SACCO Standard"

### SASRA Requirements (Met via GL)
✅ **Trial Balance** (Dr = Cr check)
✅ **Balance Sheet** (Assets = Liabilities + Equity)
✅ **Income Statement** (with tax calculation)
✅ **Cash Flow** (receipts vs payments)
✅ **Member-level detail** (member statements)
✅ **Portfolio quality** (loan register + PAR tracking)
✅ **Reserve tracking** (statutory, loan loss)
✅ **Capital adequacy** (net worth monitoring)

### CBK Compliance (If applicable)
✅ **Dual currency support** (if needed)
✅ **Audit trail** (who changed GL accounts)
✅ **Segregation of duties** (roles for treasurer, manager, admin)
✅ **Period closing** (fiscal period management)

### Auditor-Ready
✅ **GL reconciliation** (operational data ↔ GL)
✅ **Adjusting entries** (manual GL entries with justification)
✅ **Variance analysis** (budget vs actual)
✅ **Supporting detail** (drill down from GL to transactions)

---

## Why This Approach Works

1. **Flexible**: GL accounts are config, not code
2. **Auditable**: Every GL entry has a source (calc config or manual)
3. **Scalable**: Add reports by creating new GL groupings
4. **Compliant**: Built on standard accounting structure
5. **Operational**: Uses your existing transaction data
6. **Future-proof**: New data types map to new GL accounts automatically

---

## Next Step

Ready to implement Phase 1 (GL Mapping Foundation)?

Once Phase 1 is live:
- Trial balance generates correctly
- All subsequent phases stack on top with minimal effort
- Each new report just regroups the same GL data

This is the **accounting backbone your SACCO needs**.

