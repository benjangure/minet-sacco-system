# Comprehensive System Reports Audit

**Date Generated**: June 8, 2026  
**Scope**: Complete inventory of all reports in the system, their data sources, and configuration  
**Purpose**: Support design and implementation of configurable GL report system

---

## Executive Summary

The system currently has **14 distinct report types** across multiple services and controllers. Reports fall into three categories:

1. **Operational Reports** - Data from member transactions, loans, and accounts (auto-captured)
2. **GL/Accounting Reports** - Data from General Ledger accounts (mixed: auto-calculated & manual entry)
3. **Compliance Reports** - SASRA regulatory requirements (calculated from operational data)

---

## SECTION 1: OPERATIONAL REPORTS (ReportsService)

These reports use **auto-captured** operational data from transactions, accounts, and loans.

### 1.1 CASHBOOK REPORT
**Location**: `ReportsService.generateCashbook()`  
**Endpoint**: `GET /api/reports/cashbook`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `TransactionRepository.findAll()`
- Tables: `transaction`, `account`, `member`

**Filters**:
- Date Range (startDate, endDate) ✓ Required
- Member Number (optional)
- Transaction Type (optional) - DEPOSIT, WITHDRAWAL, LOAN_REPAYMENT, LOAN_DISBURSEMENT
- Account Type (optional) - SAVINGS, SHARES

**Fields Displayed**:
- Date
- Transaction Type
- Member Number & Name
- Account Type
- Amount
- Description
- **Totals**: Total Deposits, Total Withdrawals, Total Repayments, Net Cash

**Data Capture**: **AUTO** - All data from `Transaction` entity recorded when posted  
**Manual Entry**: NO

---

### 1.2 TRIAL BALANCE REPORT (Member-focused)
**Location**: `ReportsService.generateTrialBalance()`  
**Endpoint**: `GET /api/reports/trial-balance`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `AccountRepository.findAll()`, `LoanRepository.findAll()`
- Tables: `account`, `loan`, `member`

**Filters**:
- Member Number (optional)
- Account Type (optional)

**Fields Displayed**:
- Member Number & Name
- Account Type (SAVINGS, SHARES, LOAN)
- Debit / Credit Amounts
- Balance
- **Totals**: Total Debits, Total Credits, Is Balanced (boolean)

**Accounting Logic**:
- Member account balances = CREDITS (liabilities in SACCO)
- Loan outstanding balances = DEBITS (assets in SACCO)

**Data Capture**: **AUTO** - Member transactions auto-posted to accounts  
**Manual Entry**: NO

---

### 1.3 BALANCE SHEET REPORT (Member-focused)
**Location**: `ReportsService.generateBalanceSheet()`  
**Endpoint**: `GET /api/reports/balance-sheet`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `AccountRepository.findAll()` (filter by type), `LoanRepository.findAll()` (status DISBURSED/APPROVED)
- Tables: `account`, `loan`, `member`

**Fields Displayed**:
- **Assets**: Sum of outstanding loan balances
- **Liabilities**: Sum of member savings + shares
- **Equity**: Assets - Liabilities
- **Equation**: Assets = Liabilities + Equity

**Data Capture**: **AUTO** - From member transaction ledgers  
**Manual Entry**: NO  
**Weakness**: This is member-based, not GL-based. Does not include:
- Bank fees
- Interest accruals
- Equipment/assets
- Adjustments
- Manual GL entries

---

### 1.4 MEMBER STATEMENT REPORT
**Location**: `ReportsService.generateMemberStatement()`  
**Endpoint**: `GET /api/reports/member-statement/{memberId}`  
**Access**: ADMIN, TREASURER, LOAN_OFFICER, CUSTOMER_SUPPORT, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `AccountRepository.findByMemberId()`, `TransactionRepository.findAll()` (filter by account & date)
- Tables: `transaction`, `account`

**Filters**:
- Member ID ✓ Required
- Date Range (startDate, endDate) ✓ Required

**Fields Displayed**:
- Transaction Date
- Account Type
- Transaction Type
- Amount
- Description
- **Current Balances** by account type
- **Totals**: Total Deposits, Total Withdrawals

**Data Capture**: **AUTO** - Member transaction history  
**Manual Entry**: NO

---

### 1.5 LOAN REGISTER REPORT
**Location**: `ReportsService.generateLoanRegister()`  
**Endpoint**: `GET /api/reports/loan-register`  
**Access**: ADMIN, TREASURER, LOAN_OFFICER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `LoanRepository.findAll()`, `LoanRepaymentRepository.getTotalRepaidAmount()`
- Tables: `loan`, `loan_repayment`, `member`, `loan_product`

**Filters**:
- Member Number (optional)
- Loan Status (optional) - PENDING, APPROVED, DISBURSED, COMPLETED, REJECTED
- Loan Product (optional)

**Fields Displayed**:
- Loan Number
- Member Number & Name
- Loan Product
- Original Amount
- Interest Rate
- Term (months)
- Monthly Repayment
- Status
- Application / Approval / Disbursement Dates
- Outstanding Balance
- **Totals**: Total Loans Issued, Total Outstanding, Total Repaid

**Data Capture**: **AUTO** - All from loan & repayment records  
**Manual Entry**: NO

---

## SECTION 2: GL/ACCOUNTING REPORTS

These reports are designed to track accounting transactions and GL account balances.

### 2.1 TRIAL BALANCE REPORT (GL-based) [NEW]
**Location**: `GLCalculationService.generateTrialBalance()`, `BalanceSheetService`, `IncomeStatementService`  
**Endpoint**: `GET /api/gl/trial-balance`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF (via `ReportExportService.exportTrialBalanceTo*()`)

**Data Source**:
- Queries: `GLAccountRepository.findByAccountTypeAndIsActiveTrue()` (for each type)
- Calculation: `GLCalculationService.calculateGLAccountBalance()`
- Tables: `gl_account`, `gl_manual_entry` (for MANUAL_ENTRY balanceCalculationType)

**Fields Displayed**:
- GL Account Code
- GL Account Name
- Amount (balance as of date)
- **Filtered**: Only accounts with non-zero balance
- **Totals**: Total Debits, Total Credits

**GL Account Configuration**:
```
{
  code: string,
  name: string,
  accountType: ASSET | LIABILITY | EQUITY | REVENUE | EXPENSE,
  balanceCalculationType: AGGREGATION | MANUAL_ENTRY,
  calculationConfig: JSON (SQL/formula for AGGREGATION type),
  isActive: boolean,
  displayOrder: int
}
```

**Data Capture**:
- **AGGREGATION type**: `balanceCalculationType = AGGREGATION` → Data auto-calculated from operational data (loans, accounts, transactions) using `calculationConfig` formula
- **MANUAL_ENTRY type**: `balanceCalculationType = MANUAL_ENTRY` → Data from `GLManualEntry` table (treasurer-entered)

**Status**: Status of approach in implementation

---

### 2.2 BALANCE SHEET REPORT (GL-based)
**Location**: `BalanceSheetService.generateBalanceSheet()`  
**Endpoint**: `GET /api/gl/balance-sheet`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- GL Accounts by type: ASSET, LIABILITY, EQUITY
- Calculation: `GLCalculationService.calculateGLAccountBalance()` for each account

**Fields Displayed**:
- **Assets Section**: Code, Name, Amount (each ASSET GL account)
- **Liabilities Section**: Code, Name, Amount (each LIABILITY GL account)
- **Equity Section**: Code, Name, Amount (each EQUITY GL account)
- **Validation**: Assets = Liabilities + Equity

**Data Capture**: Mixed
- **AGGREGATION accounts**: Auto-calculated from operational data
- **MANUAL_ENTRY accounts**: Manually entered by treasurer

---

### 2.3 INCOME STATEMENT REPORT (GL-based)
**Location**: `IncomeStatementService.generateIncomeStatement()`  
**Endpoint**: `GET /api/gl/income-statement`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- GL Accounts by type: REVENUE, EXPENSE
- Calculation: `GLCalculationService.calculateGLAccountBalance()` for each account
- Date Range: `fromDate` to `toDate` (defaults to current month)

**Fields Displayed**:
- **Revenue Section**: Code, Name, Amount (each REVENUE account)
- **Expense Section**: Code, Name, Amount (each EXPENSE account)
- **Calculation**: Net Income = Total Revenues - Total Expenses

**Data Capture**: Mixed
- **AGGREGATION accounts**: Auto-calculated
- **MANUAL_ENTRY accounts**: Manually entered by treasurer

---

## SECTION 3: MANUAL GL ENTRY SYSTEM

### 3.1 GL MANUAL ENTRY WORKFLOW
**Location**: `GLManualEntryService`, `GLManualEntryRepository`  
**Controllers**: `GLController`

**Purpose**: Allow treasurer to enter non-operational GL entries (e.g., bank fees, interest accruals, adjustments)

**API Endpoints**:
- `POST /api/gl/manual-entries` - Create new manual entry (status: PENDING)
- `GET /api/gl/manual-entries/pending` - List pending approval
- `GET /api/gl/manual-entries` - List all
- `GET /api/gl/manual-entries?glAccountId=X` - Filter by account
- `PUT /api/gl/manual-entries/{id}/approve` - Admin approves (status: APPROVED)
- `PUT /api/gl/manual-entries/{id}/reject` - Admin rejects (status: REJECTED)
- `DELETE /api/gl/manual-entries/{id}` - Delete entry

**Fields in GLManualEntry**:
```
{
  id: Long,
  glAccount: GLAccount (reference),
  description: String,
  amount: BigDecimal,
  entryDate: LocalDate,
  status: PENDING | APPROVED | REJECTED,
  createdBy: User (treasurer),
  createdAt: LocalDateTime,
  approvedBy: User (admin),
  approvalDate: LocalDateTime,
  rejectReason: String
}
```

**Workflow**:
1. Treasurer creates entry → status = PENDING
2. Only APPROVED entries are included in GL calculations
3. Admin approves/rejects
4. Approved entries auto-included in trial balance, balance sheet, income statement

---

## SECTION 4: GUARANTOR & LOAN ELIGIBILITY REPORTS

### 4.1 GUARANTOR REPORT
**Location**: `GuarantorReportService.generateGuarantorReport()`  
**Endpoint**: `GET /api/reports/guarantor/{memberId}`  
**Access**: ADMIN, TREASURER, AUDITOR, LOAN_OFFICER  
**Export**: Excel, PDF

**Data Source**:
- Queries: `MemberRepository`, `AccountRepository`, `GuarantorRepository`
- Tables: `member`, `account`, `guarantor`, `loan`

**Fields Displayed**:
- Member Number, Name, Status
- Total Savings (sum all account balances)
- Frozen Self Guarantee Amount (sum of self-guarantee pledges with ACTIVE status)
- Available Savings = Total Savings - Frozen Amount
- Total Pledge Amount (as external guarantor)
- Available Guarantorship Capacity = Available Savings - Total Pledges
- List of Loans They're Guaranteeing:
  - Loan Number, Borrower, Loan Amount, Outstanding Balance
  - Guarantor Pledge Amount, Repayment Progress, Status

**Data Capture**: **AUTO** - Calculated from member accounts and guarantor records  
**Manual Entry**: NO (guarantor pledges manually assigned during loan approval)

---

### 4.2 GUARANTOR REPORT (ALL MEMBERS SUMMARY)
**Location**: `GuarantorReportService.generateGuarantorReportAll()`  
**Endpoint**: `GET /api/reports/guarantor/all`  
**Access**: ADMIN, TREASURER, AUDITOR, LOAN_OFFICER

**Data Source**: Same as individual report, aggregated for all members

**Fields Displayed**:
- Summary table: Member, Available Savings, Guarantorship Capacity, # of Loans Guaranteeing

**Data Capture**: **AUTO**

---

### 4.3 LOAN ELIGIBILITY REPORT
**Location**: `LoanEligibilityReportService.generateLoanEligibilityReport()`  
**Endpoint**: `GET /api/reports/loan-eligibility/{memberId}`  
**Access**: ADMIN, TREASURER, AUDITOR, LOAN_OFFICER, CUSTOMER_SUPPORT  
**Export**: Excel, PDF

**Data Source**:
- Queries: `MemberRepository`, `AccountRepository`, `GuarantorRepository`, `LoanRepository`
- Tables: `member`, `account`, `guarantor`, `loan`

**Calculation Logic**:
```
Savings Balance = SUM(account.balance WHERE accountType = SAVINGS)
Frozen Amount = SUM(guarantor.pledgeAmount WHERE self_guarantee = true AND status = ACTIVE)
Available Savings = Savings Balance - Frozen Amount

Gross Eligibility = Available Savings × 3
Outstanding Loan Balance = SUM(loan.outstandingBalance WHERE status = DISBURSED)
Remaining Eligibility = Gross Eligibility - Outstanding Loan Balance

Months as Member = months since joined
Months Contributed = member.consecutiveMonthsCounter

Eligibility Status = ELIGIBLE if:
  - Member status = ACTIVE
  - Months Contributed ≥ 6 months
  - Available Savings > 0
  - Remaining Eligibility > 0
Otherwise: NOT_ELIGIBLE
```

**Fields Displayed**:
- Member Number, Name, Status, Date Joined
- Months as Member, Months Contributed
- Savings Balance
- Frozen Amount
- Available Savings
- Gross Eligibility
- Outstanding Loan Balance
- Remaining Eligibility
- Eligibility Status & Reason

**Data Capture**: **AUTO**

---

## SECTION 5: COMPLIANCE REPORTS (SASRAComplianceReportService)

These reports calculate SACCO Societies Regulatory Authority (SASRA) compliance ratios.

### 5.1 PORTFOLIO AT RISK (PAR) REPORT
**Location**: `SASRAComplianceReportService.generatePARReport()`  
**Endpoint**: `GET /api/reports/sasra/par`  
**Access**: ADMIN, AUDITOR, TREASURER  
**Export**: Excel, PDF

**Data Source**:
- Queries: `LoanRepository.findAll()` (filter status = DISBURSED)
- Tables: `loan`, `member`

**Calculation**:
```
For each DISBURSED loan:
  - expectedEndDate = disbursementDate + termMonths
  - daysOverdue = today - expectedEndDate
  - If daysOverdue ≥ 30: amount → PAR30
  - If daysOverdue ≥ 90: amount → PAR90

PAR30 Ratio = PAR30 Amount / Total Portfolio × 100%
PAR90 Ratio = PAR90 Amount / Total Portfolio × 100%
```

**Fields Displayed**:
- Total Loans Count
- Total Portfolio Amount
- PAR30 Amount & Ratio
- PAR90 Amount & Ratio
- Compliance Status (PASS if PAR30 < 5%, PAR90 < 2%)

**Data Capture**: **AUTO** - Calculated from loan records  
**Manual Entry**: NO  
**SASRA Requirement**: PAR30 < 5% for good standing

---

### 5.2 CAPITAL ADEQUACY REPORT
**Location**: `SASRAComplianceReportService.generateCapitalAdequacyReport()`  
**Endpoint**: `GET /api/reports/sasra/capital-adequacy`  
**Access**: ADMIN, AUDITOR, TREASURER  
**Export**: Excel, PDF

**Data Source**:
- Queries: `LoanRepository`, `AccountRepository`
- Tables: `loan`, `account`

**Calculation**:
```
Total Assets = SUM(loan.outstandingBalance WHERE status = DISBURSED/APPROVED)
Core Capital = SUM(account.balance WHERE accountType = SHARES)
Institutional Capital = SUM(account.balance WHERE accountType = SAVINGS)

Core Capital Ratio = Core Capital / Total Assets × 100%
Institutional Capital Ratio = Institutional Capital / Total Assets × 100%
```

**Fields Displayed**:
- Total Assets
- Core Capital & Ratio
- Institutional Capital & Ratio
- Compliance Status (Core ≥ 10%, Institutional ≥ 8%)

**Data Capture**: **AUTO**  
**SASRA Requirement**: Core capital ≥ 10%, Institutional capital ≥ 8%

---

### 5.3 PROVISION FOR BAD DEBTS REPORT
**Location**: `SASRAComplianceReportService.generateProvisionForBadDebtsReport()`  
**Endpoint**: `GET /api/reports/sasra/provision-bad-debts`  
**Access**: ADMIN, AUDITOR, TREASURER  
**Export**: Excel, PDF

**Data Source**:
- Queries: `LoanRepository.findAll()` (filter status = DISBURSED)
- Tables: `loan`

**Calculation** (SASRA Provisioning Matrix):
```
For each DISBURSED loan:
  daysOverdue = today - (disbursementDate + termMonths)
  
  - Current (daysOverdue ≤ 0): provision = outstanding × 1%
  - 1-3 months overdue: provision = outstanding × 25%
  - 3-12 months overdue: provision = outstanding × 50%
  - 12+ months overdue: provision = outstanding × 100% (full write-off)
```

**Fields Displayed**:
- Current Loans: Count, Provision Amount
- Overdue 1-3 Months: Count, Provision Amount
- Overdue 3-12 Months: Count, Provision Amount
- Overdue 12+ Months: Count, Provision Amount
- Total Provision

**Data Capture**: **AUTO**  
**Limitation**: Provisions are calculated but not yet journaled to GL accounts. These need to be entered as MANUAL_ENTRY GL entries to appear in financial statements.

---

### 5.4 SASRA COMPLIANCE REPORT (COMPOSITE)
**Location**: `SASRAComplianceReportService.generateSASRAComplianceReport()`  
**Endpoint**: `GET /api/reports/sasra/compliance`  
**Access**: ADMIN, AUDITOR, TREASURER  
**Export**: Excel, PDF

**Data Source**: Combines all four SASRA reports above

**Additional Ratios**:
```
Liquidity Ratio = Liquid Assets (Savings) / Total Liabilities × 100%
  → Compliance: ≥ 20%

Savings-to-Loans Ratio = Total Savings / Total Loans
  → Compliance: ≥ 1 (savings cover all loans)
```

**Fields Displayed**:
- PAR 30 Ratio & Compliance
- PAR 90 Ratio & Compliance
- Core Capital Ratio & Compliance
- Institutional Capital Ratio & Compliance
- Liquidity Ratio & Compliance
- Savings-to-Loans Ratio & Compliance
- **Overall Compliance Status** (COMPLIANT if all ratios pass, else NON-COMPLIANT)

**Data Capture**: **AUTO**

---

## SECTION 6: PROFIT & LOSS REPORT

### 6.1 PROFIT & LOSS REPORT
**Location**: `ProfitLossReportService.generateProfitLossReport()`  
**Endpoint**: `GET /api/reports/profit-loss`  
**Access**: ADMIN, AUDITOR, TREASURER  
**Export**: Excel, PDF

**Data Source**:
- Queries: `LoanRepository.sumInterestIncomeInPeriod()`, `TransactionRepository.sumByDescriptionKeywordInPeriod()`
- Tables: `loan`, `transaction`

**Revenue Calculation**:
```
Interest From Loans = SUM(loan interest calculated in period) [Query-based]
Interest From Savings = 0 (future enhancement)
Total Interest Income = Above + Above

Loan Processing Fees = SUM(transaction.amount WHERE description LIKE "loan fee")
Account Maintenance Fees = SUM(transaction.amount WHERE description LIKE "account maintenance")
Other Fees = SUM(transaction.amount WHERE description LIKE "fee")
Total Fees & Charges = Above + Above + Above

Other Income = SUM(transaction.amount WHERE description LIKE "miscellaneous income")

Total Revenue = Total Interest + Total Fees + Other Income
```

**Expense Calculation**:
```
Salaries = SUM(transaction.amount WHERE description LIKE "salary")
Rent = SUM(transaction.amount WHERE description LIKE "rent")
Utilities = SUM(transaction.amount WHERE description LIKE "utilities")
Other Operating = SUM(transaction.amount WHERE description LIKE "operational")
Operating Expenses Total = Above + Above + Above + Above

Loan Loss Provisions = SUM(loan.provisioning in period) [Query-based]

Other Expenses = SUM(transaction.amount WHERE description LIKE "miscellaneous expense")

Total Expenses = Operating + Provisions + Other
```

**Calculation**:
```
Net Profit/Loss = Total Revenue - Total Expenses
Profit Margin % = (Net Profit/Loss / Total Revenue) × 100%
```

**Fields Displayed**:
- Period (start date, end date)
- Revenue Detail (interest, fees, other), Total Revenue
- Expense Detail (operating, provisions, other), Total Expenses
- Net Profit/Loss
- Profit Margin %

**Data Capture**: **AUTO** - From transaction descriptions  
**Limitation**: 
- Relies on transaction descriptions being consistent (e.g., "salary", "rent", "loan fee")
- Cannot capture non-transaction GL items (accruals, adjustments)
- Provisions calculated but not yet integrated with GL system

---

## SECTION 7: WITHDRAWAL MONITORING REPORT

### 7.1 WITHDRAWAL MONITORING REPORT
**Location**: `WithdrawalMonitoringReportService.generateWithdrawalMonitoringReport()`  
**Endpoint**: `GET /api/reports/withdrawal-monitoring`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `TransactionRepository.findAll()` (filter type = WITHDRAWAL)
- Tables: `transaction`, `account`, `member`, `user`

**Filters**:
- Date Range (startDate, endDate) ✓ Required
- Member Number (optional)
- Withdrawal Method (optional) - extracted from transaction description
- Transaction Status (optional) - extracted from transaction description

**Fields Displayed**:
- Transaction ID
- Member Number & Name
- Account Type
- Withdrawal Amount
- Transaction Date & Time
- Withdrawal Method
- Processed By (user)
- Transaction Status
- Account Balance Before & After
- Remaining Balance

**Summary Totals**:
- Total Withdrawal Amount
- Total Number of Withdrawals
- Average Withdrawal Amount

**Data Capture**: **AUTO** - From transaction records  
**Manual Entry**: NO

---

## SECTION 8: MONTHLY CONTRIBUTION TRACKING REPORT

### 8.1 MONTHLY CONTRIBUTION TRACKING REPORT
**Location**: `MonthlyContributionTrackingService.generateMonthlyContributionTrackingReport()`  
**Endpoint**: `GET /api/reports/monthly-contribution-tracking`  
**Access**: ADMIN, TREASURER, AUDITOR  
**Export**: Excel, PDF

**Data Source**:
- Queries: `BulkBatchRepository`, `BulkTransactionItemRepository`
- Tables: `bulk_batch`, `bulk_transaction_item`, `member`, `account`

**Purpose**: Track bulk contribution upload batches and their processing status

**Filters**:
- Date Range (startDate, endDate) ✓ Required
- Batch Status (optional) - PENDING, PROCESSING, PROCESSED, FAILED, PARTIAL_SUCCESS

**Fields Displayed**:
- Batch Information (ID, upload date, processed date, status)
- Summary by batch (total items, successful, failed)
- Item details (member, account, amount, transaction status)

**Data Capture**: **AUTO** - From bulk processing records  
**Manual Entry**: NO (bulk contributions manually uploaded, but system processes automatically)

---

## SECTION 9: AUDIT REPORTS SECTION

### 9.1 AUDIT TRAIL REPORT
**Location**: `AuditReportController`, `AuditService`  
**Frontend**: `AuditReports.tsx`, `AuditTrail.tsx`  
**Endpoint**: Likely `GET /api/audit/logs` or similar

**Purpose**: Track all system changes and user actions for compliance

**Data Source**:
- Queries: `AuditLogRepository`
- Tables: `audit_log`

**Data Capture**: **AUTO** - System automatically logs all changes  
**Manual Entry**: NO

---

## SECTION 10: REPORT EXPORT INFRASTRUCTURE

### 10.1 ReportExportService
**Location**: `ReportExportService`

**Purpose**: Export all reports to Excel and PDF formats

**Export Methods** (implemented):
- `exportCashbookToExcel()` / `exportCashbookToPdf()`
- `exportTrialBalanceToExcel()` / `exportTrialBalanceToPdf()`
- `exportBalanceSheetToExcel()` / `exportBalanceSheetToPdf()`
- `exportMemberStatementToExcel()` / `exportMemberStatementToPdf()`
- `exportLoanRegisterToExcel()` / `exportLoanRegisterToPdf()`
- `exportProfitLossToExcel()` / `exportProfitLossToPdf()`
- `exportWithdrawalMonitoringToExcel()` / `exportWithdrawalMonitoringToPdf()`
- `exportGuarantorReportToExcel()` / `exportGuarantorReportToPdf()`
- `exportLoanEligibilityToExcel()` / `exportLoanEligibilityToPdf()`
- `exportMonthlyContributionTrackingToExcel()` / `exportMonthlyContributionTrackingToPdf()`

### 10.2 SASRAReportExportService
**Location**: `SASRAReportExportService`

**Export Methods**:
- `exportPARToExcel()` / `exportPARToPdf()`
- `exportCapitalAdequacyToExcel()` / `exportCapitalAdequacyToPdf()`
- `exportProvisionForBadDebtsToExcel()` / `exportProvisionForBadDebtsToPdf()`
- `exportSASRAComplianceToExcel()` / `exportSASRAComplianceToPdf()`

---

## SECTION 11: FRONTEND REPORT PAGES

### 11.1 Reports.tsx (Main Dashboard)
**Location**: `minetsacco-main/src/pages/Reports.tsx`

**Features**:
- Dashboard with multiple report sections
- Real-time report generation
- Export buttons (Excel, PDF)
- Date pickers for filtering
- Member/Loan filters

**Reports Accessible**:
- Cashbook
- Trial Balance
- Balance Sheet
- Member Statement
- Loan Register
- Guarantor Reports
- Loan Eligibility
- Monthly Contribution Tracking

---

### 11.2 ProfitLossReport.tsx
**Location**: `minetsacco-main/src/pages/ProfitLossReport.tsx`

**Features**:
- Dedicated P&L report page
- Date range selection
- Revenue/Expense breakdown visualization
- Export functionality

---

### 11.3 AuditReports.tsx
**Location**: `minetsacco-main/src/pages/AuditReports.tsx`

**Features**:
- Audit log viewing
- SASRA compliance reports
- Audit trail tracking

---

### 11.4 GLManualEntries.tsx [NEW]
**Location**: `minetsacco-main/src/pages/GLManualEntries.tsx`

**Features**:
- Create manual GL entries
- View pending approvals
- Approve/reject entries
- Filter by account

---

## SECTION 12: DATA CAPTURE SUMMARY

### 12.1 Auto-Captured Data (System Records Everything)
✓ Member transactions (deposits, withdrawals)  
✓ Loan disbursements and repayments  
✓ Guarantor pledges and releases  
✓ Account balance changes  
✓ User actions (audit trail)  
✓ Bulk uploads and processing status  

**Sources**: `Transaction`, `Account`, `Loan`, `LoanRepayment`, `Guarantor`, `AuditLog`, `BulkBatch` tables

---

### 12.2 Manually Entered Data (Via UI)
✓ Member information (name, ID, contact)  
✓ Loan products (product name, interest rate, terms)  
✓ Loan applications (amount, product, purpose)  
✓ Guarantor pledges (amount, member)  
✓ Bulk member uploads (contributions, deposits)  
✓ **GL Manual Entries** (NEW) - Bank fees, interest accruals, adjustments, etc.  

**Sources**: User inputs via web forms and APIs

---

### 12.3 Calculated/Derived Data (From Formulas)
✓ Interest calculations (loan interest earned)  
✓ Eligibility assessments (loan eligibility based on savings × 3)  
✓ Guarantorship capacity (available savings - pledges)  
✓ GL account balances (from `calculationConfig` or manual entries)  
✓ SASRA compliance ratios (PAR, capital adequacy, provisions, liquidity)  
✓ P&L components (revenue from interest + fees, expenses from transactions)  

**Calculated in**: Service layer (`*Service.java` classes)

---

## SECTION 13: PROBLEM IDENTIFICATION

### Issue 1: Hardcoded GL Accounts in Migrations
**Current State**: GL accounts created via migration scripts (`V116__Create_GL_Tables.sql`, `V117__Populate_GL_Accounts.sql`)

**Problem**: 
- Cannot add new GL line items without database migration
- Treasurer cannot configure reports on-demand
- Changes require code deployment

**Solution Needed**: Move to database configuration managed by UI

---

### Issue 2: GL Reports vs Member-Based Reports Confusion
**Current State**: Two parallel systems:
- Member-based trial balance/balance sheet (ReportsService)
- GL-based trial balance/balance sheet (BalanceSheetService, IncomeStatementService)

**Problem**:
- Member-based system only captures operational data (loans, deposits)
- Cannot include non-operational GL entries (fees, accruals, adjustments)
- GL system is newer but not fully integrated with existing reports

**Solution Needed**: Consolidate to GL-centric reporting with mixed data sources

---

### Issue 3: P&L Report Relies on Transaction Descriptions
**Current State**: P&L classifies expenses by keyword matching in transaction description

**Problem**:
- Inconsistent descriptions lead to misclassification
- Cannot handle items not in transaction system (accruals, adjustments)
- No manual override capability

**Solution Needed**: Link P&L items to GL accounts instead of transactions

---

### Issue 4: Provisions Not Journaled
**Current State**: Provision for Bad Debts calculated but not posted to GL

**Problem**:
- Balance sheet doesn't reflect provisions (understates liabilities)
- System cannot track provisioning history
- Provisions disappear if system recalculated

**Solution Needed**: Auto-create or require manual GL entries for provisions

---

### Issue 5: Missing GL Sub-Types
**Current State**: GL accounts have type (ASSET, LIABILITY, etc.) but no sub-classification

**Problem**:
- Cannot distinguish between different types of assets
- No organization of GL accounts by category
- Balance sheet line items not organized

**Solution Needed**: Add GL account categories/sub-types

---

## SECTION 14: RECOMMENDED APPROACH FOR CONFIGURABLE GL SYSTEM

### Vision
Treasurer configures GL line items and their relationships to reports. System auto-calculates some items, accepts manual entry for others.

### Architecture

#### Database Changes
1. **`gl_account_category`** - Grouping GL accounts (e.g., "Current Assets", "Fixed Assets", "Operating Expenses")
2. **`report_configuration`** - Define which GL accounts appear on which reports
3. **`gl_account_mapping`** - Map GL accounts to data sources (transaction keywords, calculations, manual entry)
4. **`gl_calculation_rule`** - Rules for AGGREGATION type accounts (SQL, formulas)

#### UI Pages
1. **GL Account Configuration** - Create/edit GL accounts with categories
2. **Report Line Item Configuration** - Assign GL accounts to reports, set display order
3. **GL Entry Data Page** - Treasurer enters monthly values for MANUAL_ENTRY accounts
4. **Report Generation** - Reports dynamically built from configuration

#### API Endpoints
```
POST /api/gl/accounts - Create account
PUT /api/gl/accounts/{id} - Update account
DELETE /api/gl/accounts/{id} - Deactivate account
GET /api/gl/accounts?type=ASSET - List by type

POST /api/gl/categories - Create category
PUT /api/gl/categories/{id} - Update category

POST /api/gl/report-configs - Configure report
GET /api/gl/report-configs/{reportType} - Get configuration

POST /api/gl/calculation-rules - Define calculation
PUT /api/gl/calculation-rules/{id} - Update rule
```

#### Data Flow
```
1. Treasurer creates GL account (Code 3100, "Bank Fees", Type EXPENSE, Category "Operating Expenses", Source MANUAL_ENTRY)
2. Treasurer assigns to Trial Balance & Income Statement reports
3. Report configuration saved with display order
4. Treasurer navigates to "GL Data Entry" page
5. Enters monthly value for "Bank Fees": 5,000
6. Entry saved to GLManualEntry table (status PENDING)
7. Admin approves entry
8. Trial Balance & Income Statement automatically include "Bank Fees" line item
```

---

## SECTION 15: REPORT CONFIGURATION SPECIFICATION

### Configuration Data Model

```javascript
ReportConfiguration {
  id: number,
  reportType: string,  // "TRIAL_BALANCE" | "BALANCE_SHEET" | "INCOME_STATEMENT"
  glAccountId: number,
  lineItemName: string,
  displayOrder: number,
  side: "DEBIT" | "CREDIT",  // Which side does it appear on?
  isActive: boolean,
  createdAt: Date,
  updatedBy: User
}

GLAccountMapping {
  id: number,
  glAccountId: number,
  dataSource: "AGGREGATION" | "MANUAL_ENTRY" | "CALCULATION",
  calculationFormula: string,  // If CALCULATION type
  transactionKeyword: string,  // If keyword-based
  isActive: boolean
}
```

### Example Configuration

**Report: Trial Balance**

| Line Item | GL Code | GL Name | Data Source | Debit | Credit | Display Order |
|-----------|---------|---------|------------|-------|--------|---------------|
| Cash in Bank | 1010 | Bank Account | AGGREGATION (loan portfolio total - outstandings) | Yes | | 1 |
| Savings | 2010 | Member Savings | AGGREGATION (sum account balances) | | Yes | 2 |
| Shares | 2020 | Member Shares | AGGREGATION (sum shares) | | Yes | 3 |
| Bank Fees | 3100 | Bank Fees | MANUAL_ENTRY (treasurer enters) | Yes | | 4 |
| Interest Accrual | 4050 | Interest Accrued | AGGREGATION (calculated from loans) | | Yes | 5 |

---

## CONCLUSION

The system has **14 main reports** across operational, GL, and compliance categories. Reports use **mixed data sources**: auto-calculated from operational data, manually entered GL values, and derived calculations. 

To achieve the treasurer's vision of flexible GL reporting:

1. ✓ GL Manual Entry System foundation is in place
2. 🔧 Need to consolidate Report Configuration UI
3. 🔧 Need GL Account Configuration UI
4. 🔧 Need GL Data Entry page for monthly manual values
5. 🔧 Need to distinguish between AGGREGATION and MANUAL_ENTRY GL accounts in reporting
6. 🔧 Need to link P&L report to GL accounts instead of transactions

**Current Status**: GL infrastructure 50% complete. Manual entry system works. Needs UI & configuration layer.

---

**Report Generated By**: Comprehensive System Audit  
**Audit Date**: June 8, 2026  
**Next Steps**: Design & implement Report Configuration and GL Data Entry UI
