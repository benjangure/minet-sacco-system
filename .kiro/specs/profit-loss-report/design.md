# Profit & Loss Report - Design Document

## Overview
The Profit & Loss (Income Statement) Report provides a comprehensive view of the SACCO's financial performance over a specified period, showing all revenues, expenses, and resulting profit or loss.

## High-Level Design

### Report Structure
```
MINET SACCO - PROFIT & LOSS STATEMENT
Period: [Start Date] to [End Date]

REVENUE
├── Interest Income
│   ├── Interest from Loans (disbursed loans)
│   └── Interest from Savings/Shares (if applicable)
├── Fees & Charges
│   ├── Loan Processing Fees
│   ├── Account Maintenance Fees
│   └── Other Fees
└── Other Income
    └── Miscellaneous Income

TOTAL REVENUE

EXPENSES
├── Operating Expenses
│   ├── Staff Salaries
│   ├── Office Rent
│   ├── Utilities
│   └── Other Operating Costs
├── Loan Loss Provisions
│   ├── Provision for Doubtful Debts
│   └── Write-offs
└── Other Expenses
    └── Miscellaneous Expenses

TOTAL EXPENSES

NET PROFIT/LOSS (TOTAL REVENUE - TOTAL EXPENSES)
```

### Data Sources

#### 1. Interest Income Calculation
- **Source**: `loans` table
- **Calculation**: Sum of `total_interest` for all loans with status = DISBURSED or REPAID in the period
- **Formula**: 
  ```
  Interest Income = SUM(loan.total_interest) 
  WHERE loan.status IN ('DISBURSED', 'REPAID') 
  AND loan.disbursement_date BETWEEN start_date AND end_date
  ```

#### 2. Loan Loss Provisions
- **Source**: `loans` table
- **Calculation**: Sum of outstanding balance for DEFAULTED loans
- **Formula**:
  ```
  Loan Loss Provision = SUM(loan.outstanding_balance)
  WHERE loan.status = 'DEFAULTED'
  AND loan.created_at BETWEEN start_date AND end_date
  ```

#### 3. Operating Expenses
- **Source**: `transactions` table (expense transactions)
- **Calculation**: Sum of all expense transactions in the period
- **Formula**:
  ```
  Operating Expenses = SUM(transaction.amount)
  WHERE transaction.type = 'EXPENSE'
  AND transaction.created_at BETWEEN start_date AND end_date
  ```

#### 4. Member Contributions (Revenue Recognition)
- **Source**: `transactions` table
- **Calculation**: Sum of all deposits to savings/shares accounts
- **Note**: This may be treated as liability (member funds) rather than revenue, depending on accounting policy

### API Endpoint Design

#### GET /api/reports/profit-loss
**Query Parameters:**
- `startDate` (required): Start date (YYYY-MM-DD)
- `endDate` (required): End date (YYYY-MM-DD)

**Response Structure:**
```json
{
  "success": true,
  "data": {
    "period": {
      "startDate": "2026-01-01",
      "endDate": "2026-03-12"
    },
    "revenue": {
      "interestIncome": {
        "fromLoans": 125000.00,
        "fromSavings": 5000.00,
        "total": 130000.00
      },
      "feesAndCharges": {
        "loanProcessingFees": 15000.00,
        "accountMaintenanceFees": 5000.00,
        "otherFees": 2000.00,
        "total": 22000.00
      },
      "otherIncome": 3000.00,
      "totalRevenue": 155000.00
    },
    "expenses": {
      "operatingExpenses": {
        "salaries": 40000.00,
        "rent": 15000.00,
        "utilities": 5000.00,
        "other": 10000.00,
        "total": 70000.00
      },
      "loanLossProvisions": {
        "doubtfulDebts": 25000.00,
        "writeOffs": 5000.00,
        "total": 30000.00
      },
      "otherExpenses": 5000.00,
      "totalExpenses": 105000.00
    },
    "netProfitLoss": 50000.00,
    "profitMargin": 32.26
  }
}
```

### Low-Level Design

#### Database Queries

**Query 1: Interest Income from Disbursed/Repaid Loans**
```sql
SELECT 
  SUM(l.total_interest) as interest_income
FROM loans l
WHERE l.status IN ('DISBURSED', 'REPAID')
  AND l.disbursement_date >= ? 
  AND l.disbursement_date <= ?;
```

**Query 2: Loan Loss Provisions (Defaulted Loans)**
```sql
SELECT 
  SUM(l.outstanding_balance) as loan_loss_provision
FROM loans l
WHERE l.status = 'DEFAULTED'
  AND l.created_at >= ? 
  AND l.created_at <= ?;
```

**Query 3: Operating Expenses**
```sql
SELECT 
  SUM(t.amount) as operating_expenses
FROM transactions t
WHERE t.type = 'EXPENSE'
  AND t.created_at >= ? 
  AND t.created_at <= ?;
```

**Query 4: Loan Processing Fees**
```sql
SELECT 
  SUM(t.amount) as loan_fees
FROM transactions t
WHERE t.description LIKE '%loan%fee%'
  AND t.type = 'INCOME'
  AND t.created_at >= ? 
  AND t.created_at <= ?;
```

#### Service Layer Design

**Class: ProfitLossReportService**

Methods:
1. `generateProfitLossReport(LocalDate startDate, LocalDate endDate)` → ProfitLossReportDTO
2. `calculateInterestIncome(LocalDate startDate, LocalDate endDate)` → BigDecimal
3. `calculateLoanLossProvisions(LocalDate startDate, LocalDate endDate)` → BigDecimal
4. `calculateOperatingExpenses(LocalDate startDate, LocalDate endDate)` → BigDecimal
5. `calculateFeesAndCharges(LocalDate startDate, LocalDate endDate)` → BigDecimal
6. `calculateNetProfitLoss(BigDecimal revenue, BigDecimal expenses)` → BigDecimal

#### Controller Design

**Class: ReportsController (extend existing)**

New Endpoint:
```java
@GetMapping("/profit-loss")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<ProfitLossReportDTO>> getProfitLossReport(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate
)
```

#### Frontend Design

**Component: ProfitLossReport.tsx**

Features:
- Date range picker (start and end date)
- Generate report button
- Display P&L statement in formatted table
- Export to Excel button
- Export to PDF button
- Summary cards showing:
  - Total Revenue
  - Total Expenses
  - Net Profit/Loss
  - Profit Margin %

### Data Accuracy Considerations

1. **Loan Interest Calculation**
   - Only count interest from loans with status DISBURSED or REPAID
   - Use `total_interest` field (pre-calculated at loan creation)
   - Filter by `disbursement_date` to ensure period accuracy

2. **Loan Loss Provisions**
   - Only count DEFAULTED loans
   - Use `outstanding_balance` as the provision amount
   - This represents potential loss

3. **Operating Expenses**
   - Require proper transaction categorization
   - Expenses must be marked with type = 'EXPENSE'
   - Filter by transaction date

4. **Period Filtering**
   - Use consistent date filtering across all queries
   - Include both start and end dates (inclusive)
   - Handle timezone considerations

### Correctness Properties

1. **Revenue Completeness**: All interest-bearing loans in the period are included
2. **Expense Accuracy**: All expense transactions are captured
3. **Balance Equation**: Net Profit/Loss = Total Revenue - Total Expenses
4. **Non-Negative Balances**: All component values are >= 0
5. **Period Consistency**: All data points use the same date range

