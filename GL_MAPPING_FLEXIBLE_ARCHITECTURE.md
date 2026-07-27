# GL Mapping Architecture: Flexible & Future-Proof

## Problem Statement
- Trial balance accounts need to be generated from operational data
- Data is subject to change (new loan types, account types, funds)
- Hard-coded mappings = system fragility
- Need flexibility without building full GL system

## Solution: Configurable GL Mapping Layer

### Core Principle
**GL accounts are not hard-coded. They're configuration that points to data sources.**

Each GL account says:
- "What is your name?" (Trial Balance account name)
- "What type are you?" (Asset/Liability/Revenue/Expense)
- "How do you calculate your balance?" (Query/Rule/Manual)

---

## Architecture: 3 Tables

### 1. `gl_accounts` (Configuration)
```sql
CREATE TABLE gl_accounts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(20) UNIQUE NOT NULL,              -- "LOAN_NORMAL", "MEMBER_DEPOSITS"
  name VARCHAR(255) NOT NULL,                    -- "Normal loan", "Member deposits"
  account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'),
  balance_calculation_type ENUM(
    'AGGREGATION',      -- Sum from a table
    'FORMULA',          -- Math formula on other GL accounts
    'MANUAL_ENTRY',     -- Treasurer enters it
    'COMPUTED'          -- Complex query
  ),
  calculation_config JSON NOT NULL,              -- Stores HOW to calculate
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. `gl_account_calculations` (Formula Engine)
```sql
CREATE TABLE gl_account_calculations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  calculation_name VARCHAR(255),                 -- "Disbursed Normal Loans"
  calculation_type ENUM(
    'SUM_FIELD',        -- SUM(field) from table
    'CUSTOM_QUERY',     -- Raw SQL query
    'LOOKUP',           -- Reference another GL account
    'PERCENTAGE',       -- % of another account
    'CONDITIONAL'       -- IF/THEN logic
  ),
  calculation_config JSON NOT NULL,              -- Type-specific config
  weight DECIMAL(5,2) DEFAULT 1.0,               -- Multiply by weight
  operator ENUM('+', '-', '*', '/') DEFAULT '+', -- How to combine with other calcs
  sort_order INT,
  is_active BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id)
);
```

### 3. `gl_manual_entries` (For Manual Adjustments)
```sql
CREATE TABLE gl_manual_entries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  entry_date DATE NOT NULL,
  description VARCHAR(255),
  amount DECIMAL(15,2) NOT NULL,
  is_debit BOOLEAN,
  entered_by_user_id INT,
  entry_reason ENUM(
    'ACCRUAL',          -- Proposed interest, dividends
    'ADJUSTMENT',       -- Manual correction
    'ALLOCATION'        -- Manual allocation of expense
  ),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id),
  FOREIGN KEY (entered_by_user_id) REFERENCES users(id)
);
```

---

## Implementation Pattern: Calculation Config

Each GL account's `calculation_config` is JSON that describes HOW to calculate it.

### Example 1: Simple Aggregation
```json
{
  "type": "SUM_FIELD",
  "table": "loans",
  "field": "disbursed_amount",
  "where": "loan_type = 'NORMAL' AND status != 'CANCELLED'",
  "date_field": "disbursement_date"
}
```

### Example 2: Multi-Source Sum
```json
{
  "type": "SUM_MULTIPLE",
  "sources": [
    {
      "table": "accounts",
      "field": "balance",
      "where": "account_type IN ('SAVINGS', 'SHARES')"
    },
    {
      "table": "accounts",
      "field": "frozen_savings",
      "where": "account_type = 'SAVINGS'"
    }
  ],
  "operators": ["+", "+"]
}
```

### Example 3: Query-Based
```json
{
  "type": "CUSTOM_QUERY",
  "query": "SELECT SUM(amount) FROM transactions WHERE transaction_type = 'INTEREST_RECEIVED' AND created_at >= ? AND created_at < ?"
}
```

### Example 4: With Manual Adjustment
```json
{
  "type": "AGGREGATION_WITH_MANUAL",
  "aggregation": {
    "table": "loans",
    "field": "outstanding_balance",
    "where": "status = 'ACTIVE'"
  },
  "plus_manual_from_gl_account_id": 105  -- Reference another GL account for accruals
}
```

### Example 5: Accrual Calculation
```json
{
  "type": "FORMULA",
  "formula": "(MEMBER_DEPOSITS * PROPOSED_DIVIDEND_RATE / 100)",
  "references": ["MEMBER_DEPOSITS", "PROPOSED_DIVIDEND_RATE"],
  "note": "Calculated field: Member deposits * dividend rate"
}
```

---

## Data Can Change: Handled Automatically

### Scenario 1: New Loan Type Added (e.g., "Group Loan")
**Current state**: Hard-coded query `WHERE loan_type = 'NORMAL'`
**Problem**: Group loans missing from trial balance

**Solution with flexible architecture**:
1. Treasurer goes to GL Configuration UI
2. Adds new GL account: "Group loan" (code: LOAN_GROUP)
3. Copies calculation config from "Normal loan" account
4. Changes WHERE clause: `WHERE loan_type = 'GROUP'`
5. ✅ Trial balance includes new loan type, no code change needed

### Scenario 2: New Account Type Added (e.g., "Membership")
**Same process**: Treasurer adds new GL account via UI

### Scenario 3: Business Rule Change (e.g., "Calculate loan loss reserve as 5% of outstanding")
**Current**: Hard-coded "Loan Loss reserve" value
**Solution**:
1. Treasurer modifies GL account calculation
2. Changes from MANUAL_ENTRY to FORMULA
3. Sets formula: `(LOAN_OUTSTANDING * 5 / 100)`
4. ✅ Reserve auto-calculates

### Scenario 4: New Manual Entry Categories (e.g., "Tax provision")
**Current**: Limited to predefined expenses
**Solution**:
1. Create new GL account: "Income tax"
2. Set type: MANUAL_ENTRY with reason: 'TAX_PROVISION'
3. Treasurer enters amount via UI (with audit trail)
4. ✅ Shows on trial balance

---

## Backend Service: Calculation Engine

```java
@Service
public class GLCalculationService {
  
  // Calculate a single GL account balance
  public BigDecimal calculateGLAccountBalance(Integer glAccountId, LocalDate asOfDate) {
    GLAccount account = glAccountRepository.findById(glAccountId);
    
    if (account.getCalculationType() == AGGREGATION) {
      return calculateAggregation(account.getCalculationConfig(), asOfDate);
    } else if (account.getCalculationType() == FORMULA) {
      return calculateFormula(account.getCalculationConfig(), asOfDate);
    } else if (account.getCalculationType() == MANUAL_ENTRY) {
      return sumManualEntries(glAccountId, asOfDate);
    }
    // ... more types
  }
  
  // Generate trial balance for a date
  public TrialBalanceDTO generateTrialBalance(LocalDate asOfDate) {
    List<GLAccount> activeAccounts = glAccountRepository.findActive();
    
    List<TrialBalanceLineDTO> lines = activeAccounts.stream()
      .map(acc -> new TrialBalanceLineDTO(
        acc.getCode(),
        acc.getName(),
        acc.getAccountType(),
        calculateGLAccountBalance(acc.getId(), asOfDate)  // Intelligent calculation
      ))
      .collect(toList());
    
    return new TrialBalanceDTO(asOfDate, lines, calculateTotals(lines));
  }
}
```

---

## Frontend: GL Configuration UI

**Treasurer can manage GL accounts without code changes:**

```
┌─ GL Configuration ────────────────────────┐
│                                            │
│ GL Accounts (Active: 25)                  │
│ ─────────────────────────────────────────  │
│ Code         | Name                | Type │
│ LOAN_NORMAL  | Normal loan        | Asset│
│ MEMBER_DEPS  | Member deposits    | Liab │
│ INT_LOANS    | Interest - loans   | Rev  │
│ ......                                    │
│                                            │
│ [+ Add New Account]                      │
│                                            │
└─ Select "LOAN_NORMAL" to Edit ───────────┘

┌─ Edit GL Account: Normal Loan ────────────┐
│ Code: LOAN_NORMAL (read-only)             │
│ Name: Normal loan                         │
│ Type: Asset                               │
│                                            │
│ Calculation Method: AGGREGATION           │
│ ─────────────────────────────────────────  │
│ Table: loans                              │
│ Field: disbursed_amount                   │
│ Filter: WHERE loan_type = 'NORMAL'        │
│         AND status != 'CANCELLED'         │
│                                            │
│ [Test Query]  [Save]  [Cancel]            │
│                                            │
│ Last balance: 99,629,963  (as of today)  │
└───────────────────────────────────────────┘
```

---

## Phase Implementation

### Phase 1: Foundation (3-4 hours)
- Create the 3 tables (gl_accounts, gl_account_calculations, gl_manual_entries)
- Create GLCalculationService with engine
- Build `GET /api/gl/accounts` endpoint
- Build `GET /api/reports/trial-balance` endpoint

### Phase 2: Configuration & Manual Entry (3-4 hours)
- Populate initial GL accounts from your trial balance (25 accounts)
- Create `POST /api/gl/accounts` + update + delete endpoints
- Build manual entry UI for treasurer (interest accruals, dividends, etc.)
- Add validation rules

### Phase 3: Frontend Display (2-3 hours)
- Display trial balance report
- Show Dr/Cr columns
- Group by account type
- Calculate totals

### Phase 4: Advanced (Optional, 2-3 hours)
- Formula-based calculations (5% of outstanding, etc.)
- Conditional logic
- Balance sheet generation from GL accounts
- P&L generation from GL accounts

---

## Why This Works When Data Changes

| Change Type | Old Approach | New Approach |
|-------------|-------------|-------------|
| New loan type | Code update + deployment | Treasurer adds GL account via UI |
| New account type | Code update + deployment | Treasurer adds GL account via UI |
| Business rule (e.g., reserve %) | Code update + deployment | Treasurer edits formula in UI |
| New manual category | Code update + deployment | Treasurer creates GL account + enters data |
| Trial balance format changes | Code update | Rename/reorganize GL accounts in UI |

---

## What This Enables Long-Term

Once GL accounts are configurable:
- ✅ Generate balance sheet by grouping GL accounts
- ✅ Generate P&L by filtering revenue/expense accounts
- ✅ Generate cash flow by tracking specific GL account changes
- ✅ Audit trail of changes (who added/modified GL accounts)
- ✅ Support for budget vs actual comparisons
- ✅ Support for multi-currency (future)

---

## Key Difference from Full GL System

**Full GL System** = Every transaction posts to 2+ GL accounts (journal entries)
- Complex: Requires transaction redesign
- Powerful: True double-entry accounting
- Slow: Every transaction = multiple inserts

**This Architecture** = GL accounts calculated from operational data
- Simple: No journal entries
- Good enough: Trial balance accurate, operational data clean
- Fast: Queries on existing tables

**When to upgrade to full GL**:
- When you need real-time GL ledger
- When audit requirements mandate double-entry
- When you expand beyond SACCO services

