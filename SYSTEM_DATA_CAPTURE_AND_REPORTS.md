# System Data Capture & Reports Capability

This document explains what data the SACCO system captures and what reports can be generated from that data.

---

## PART 1: WHAT DATA DOES THE SYSTEM CAPTURE?

### 1. MEMBER DATA
**Table:** `member`

**Captured Fields:**
- Member ID (unique identifier)
- Member Number (e.g., M001, M002)
- First Name, Last Name
- Email, Phone Number
- National ID
- Employment Status
- Date Joined
- Member Status (ACTIVE, INACTIVE, SUSPENDED, EXITED)
- KYC Status (PENDING, VERIFIED, REJECTED)

**What This Enables:**
- Member identification in all reports
- Member filtering in reports
- Member status tracking

---

### 2. ACCOUNT DATA
**Table:** `account`

**Captured Fields:**
- Account ID
- Member ID (links to member)
- Account Type (SAVINGS, SHARES, BENEVOLENT_FUND, DEVELOPMENT_FUND, SCHOOL_FEES, HOLIDAY_FUND, EMERGENCY_FUND)
- Current Balance
- Account Status (ACTIVE, FROZEN, CLOSED)
- Date Opened
- Date Closed (if applicable)

**What This Enables:**
- Trial Balance Report (shows all account balances)
- Balance Sheet Report (calculates total savings and shares)
- Member Statement Report (shows member's account balances)
- Capital Adequacy Report (uses shares and savings for capital calculation)

---

### 3. TRANSACTION DATA
**Table:** `transaction`

**Captured Fields:**
- Transaction ID
- Account ID (links to account)
- Transaction Type (DEPOSIT, WITHDRAWAL, LOAN_DISBURSEMENT, LOAN_REPAYMENT, INTEREST, LOAN_DEFAULT_DEBIT)
- Amount
- Transaction Date & Time
- Description (e.g., "Monthly contribution", "Loan repayment", "Salary payment")
- Status (PENDING, COMPLETED, FAILED)

**What This Enables:**
- Cashbook Report (shows all transactions by date)
- Member Statement Report (shows member's transaction history)
- Profit & Loss Report (uses transaction descriptions to categorize income/expenses)
- Account Statement Report (member's transaction history)
- Transaction History Report (member's all transactions)

---

### 4. LOAN DATA
**Table:** `loan`

**Captured Fields:**
- Loan ID
- Loan Number (e.g., LN-2026-00001)
- Member ID (links to member)
- Loan Product ID (links to loan product)
- Amount (original loan amount)
- Interest Rate (%)
- Term Months (loan duration)
- Monthly Repayment (calculated)
- Total Repayable (amount + interest)
- Outstanding Balance (remaining to be repaid)
- Status (PENDING, APPROVED, REJECTED, DISBURSED, REPAID, DEFAULTED)
- Application Date
- Approval Date
- Disbursement Date
- Interest Amount (calculated)

**What This Enables:**
- Loan Register Report (shows all loans with details)
- Balance Sheet Report (uses outstanding balance as assets)
- Trial Balance Report (shows loans as debits)
- PAR Report (calculates days overdue from disbursement date)
- Capital Adequacy Report (uses outstanding balance for asset calculation)
- Provision for Bad Debts Report (categorizes loans by overdue status)
- SASRA Compliance Report (combines all loan metrics)

---

### 5. LOAN REPAYMENT DATA
**Table:** `loan_repayment`

**Captured Fields:**
- Repayment ID
- Loan ID (links to loan)
- Amount Repaid
- Repayment Date
- Repayment Method (CASH, M_PESA, BANK_TRANSFER)
- Status (PENDING, COMPLETED, FAILED)
- Recorded By (user who recorded)

**What This Enables:**
- Loan Register Report (calculates total repaid)
- Loan Statement Report (shows member's repayments)
- Transaction History Report (shows repayment transactions)

---

### 6. GUARANTOR DATA
**Table:** `guarantor`

**Captured Fields:**
- Guarantor ID
- Loan ID (links to loan)
- Member ID (guarantor member)
- Pledge Amount (amount guaranteed)
- Status (PENDING, APPROVED, REJECTED, ACTIVE, RELEASED)
- Date Created

**What This Enables:**
- Loan Register Report (shows guarantor information)
- Member Statement Report (shows guarantor obligations)

---

### 7. AUDIT LOG DATA
**Table:** `audit_log`

**Captured Fields:**
- Log ID
- User ID (who performed action)
- Action (APPROVE, REJECT, DISBURSE, REPAY, etc.)
- Entity Type (LOAN, MEMBER, ACCOUNT, etc.)
- Entity ID (which record was affected)
- Entity Details (what changed)
- Timestamp
- Status (SUCCESS, FAILURE)
- IP Address
- User Agent

**What This Enables:**
- Audit Trail Report (shows all system actions)
- Compliance tracking
- User activity monitoring

---

### 8. LOAN PRODUCT DATA
**Table:** `loan_product`

**Captured Fields:**
- Product ID
- Product Name (e.g., "Personal Loan", "Emergency Loan")
- Description
- Interest Rate (%)
- Maximum Amount
- Minimum Amount
- Term Months
- Status (ACTIVE, INACTIVE)

**What This Enables:**
- Loan Register Report (shows loan product names)
- Loan filtering by product

---

## PART 2: WHAT REPORTS CAN BE GENERATED?

### CATEGORY A: TRANSACTION-BASED REPORTS
These reports use the `transaction` table as primary data source.

#### Report 1: CASHBOOK REPORT
**Data Used:** `transaction` table

**What It Shows:**
- Every transaction that occurred in a date range
- Organized by date
- Shows: Date, Type, Member, Amount, Description

**Calculations:**
- Total Deposits (sum of DEPOSIT + LOAN_DISBURSEMENT)
- Total Withdrawals (sum of WITHDRAWAL)
- Total Repayments (sum of LOAN_REPAYMENT)
- Net Cash (Deposits + Repayments - Withdrawals)

**Filters Available:**
- Date Range (start date to end date)
- Member Number
- Transaction Type
- Account Type

**Use Case:** Daily cash reconciliation, audit trail

---

#### Report 2: MEMBER STATEMENT REPORT
**Data Used:** `transaction` table (filtered by member) + `account` table

**What It Shows:**
- All transactions for a specific member in a date range
- Member's current account balances

**Calculations:**
- Total Deposits (for member)
- Total Withdrawals (for member)
- Current balance in each account type

**Filters Available:**
- Member ID
- Date Range

**Use Case:** Member wants to see their account history

---

#### Report 3: ACCOUNT STATEMENT (Member Portal)
**Data Used:** `transaction` table (filtered by member)

**What It Shows:**
- Member's savings account transactions
- PDF format for download

**Use Case:** Member personal record

---

#### Report 4: TRANSACTION HISTORY (Member Portal)
**Data Used:** `transaction` table (filtered by member)

**What It Shows:**
- All member transactions in date range
- PDF format for download

**Use Case:** Member personal record

---

### CATEGORY B: ACCOUNT-BASED REPORTS
These reports use the `account` table as primary data source.

#### Report 5: TRIAL BALANCE REPORT
**Data Used:** `account` table + `loan` table

**What It Shows:**
- All member accounts with their balances
- All loans with outstanding balances
- Organized as Debits (loans) and Credits (accounts)

**Calculations:**
- Total Debits (sum of loan outstanding balances)
- Total Credits (sum of account balances)
- Is Balanced? (Debits == Credits)

**Filters Available:**
- Member Number
- Account Type

**Use Case:** Accounting verification, financial audit

---

#### Report 6: BALANCE SHEET REPORT
**Data Used:** `account` table + `loan` table

**What It Shows:**
- ASSETS: Total loans outstanding
- LIABILITIES: Total member savings + shares
- EQUITY: Assets - Liabilities

**Calculations:**
- Total Assets (sum of loan outstanding balances)
- Total Savings (sum of SAVINGS accounts)
- Total Shares (sum of SHARES accounts)
- Total Liabilities (Savings + Shares)
- Equity (Assets - Liabilities)

**Filters Available:** None (shows entire SACCO)

**Use Case:** Financial position statement, annual reporting

---

### CATEGORY C: LOAN-BASED REPORTS
These reports use the `loan` table as primary data source.

#### Report 7: LOAN REGISTER REPORT
**Data Used:** `loan` table + `loan_repayment` table

**What It Shows:**
- All loans with complete details
- Loan number, member, product, amount, interest, term
- Loan status and dates
- Outstanding balance

**Calculations:**
- Total Loans Issued (sum of loan amounts)
- Total Outstanding (sum of outstanding balances)
- Total Repaid (sum of all repayments)

**Filters Available:**
- Member Number
- Loan Status
- Loan Product

**Use Case:** Loan portfolio management, member loan history

---

#### Report 8: LOAN STATEMENT (Member Portal)
**Data Used:** `loan` table + `loan_repayment` table (filtered by member)

**What It Shows:**
- Member's loans and repayment history
- PDF format for download

**Use Case:** Member personal record

---

### CATEGORY D: PROFIT & LOSS REPORTS
These reports use `transaction` table + `loan` table.

#### Report 9: PROFIT & LOSS REPORT
**Data Used:** `transaction` table + `loan` table

**What It Shows:**
- REVENUE:
  - Interest Income (from loans)
  - Loan Processing Fees (from transactions with "loan fee" in description)
  - Account Maintenance Fees (from transactions with "account maintenance" in description)
  - Other Fees (from transactions with "fee" in description)
  - Other Income (from transactions with "miscellaneous income" in description)
- EXPENSES:
  - Operating Expenses (from transactions with "salary", "rent", "utilities", "operational" in description)
  - Loan Loss Provisions (from DEFAULTED loans)
  - Other Expenses (from transactions with "miscellaneous expense" in description)
- NET PROFIT/LOSS (Revenue - Expenses)
- PROFIT MARGIN (Net Profit / Revenue * 100)

**Calculations:**
- Total Revenue (sum of all income)
- Total Expenses (sum of all expenses)
- Net Profit/Loss (Revenue - Expenses)
- Profit Margin (%)

**Filters Available:**
- Date Range

**⚠ LIMITATION:** Depends on transaction descriptions containing specific keywords. If transactions don't have these keywords, report shows zero values.

**Use Case:** Financial performance analysis, annual reporting

---

### CATEGORY E: SASRA COMPLIANCE REPORTS
These reports use `loan` table + `account` table for regulatory compliance.

#### Report 10: PORTFOLIO AT RISK (PAR) REPORT
**Data Used:** `loan` table (DISBURSED loans only)

**What It Shows:**
- PAR 30: Loans overdue 30+ days (as % of total portfolio)
- PAR 90: Loans overdue 90+ days (as % of total portfolio)
- Compliance status (PASS/FAIL)

**Calculations:**
- Days Overdue = Current Date - (Disbursement Date + Term Months)
- PAR 30 Amount = sum of outstanding balances where days overdue >= 30
- PAR 90 Amount = sum of outstanding balances where days overdue >= 90
- PAR 30 Ratio = (PAR 30 Amount / Total Portfolio) * 100
- PAR 90 Ratio = (PAR 90 Amount / Total Portfolio) * 100

**SASRA Thresholds:**
- PAR 30 must be < 5% (PASS)
- PAR 90 must be < 2% (PASS)

**Filters Available:**
- As at Date

**Use Case:** Regulatory compliance, loan quality assessment

---

#### Report 11: CAPITAL ADEQUACY REPORT
**Data Used:** `account` table + `loan` table

**What It Shows:**
- Core Capital (member shares)
- Institutional Capital (member savings)
- Total Assets (loans outstanding)
- Core Capital Ratio (%)
- Institutional Capital Ratio (%)
- Compliance status (PASS/FAIL)

**Calculations:**
- Total Assets = sum of loan outstanding balances
- Core Capital = sum of SHARES account balances
- Institutional Capital = sum of SAVINGS account balances
- Core Capital Ratio = (Core Capital / Total Assets) * 100
- Institutional Capital Ratio = (Institutional Capital / Total Assets) * 100

**SASRA Thresholds:**
- Core Capital must be >= 10% (PASS)
- Institutional Capital must be >= 8% (PASS)

**Filters Available:**
- As at Date

**Use Case:** Regulatory compliance, financial strength assessment

---

#### Report 12: PROVISION FOR BAD DEBTS REPORT
**Data Used:** `loan` table (DISBURSED loans only)

**What It Shows:**
- Current Loans (0 days overdue): 1% provision
- 1-3 months overdue: 25% provision
- 3-12 months overdue: 50% provision
- 12+ months overdue: 100% provision
- Total Provision

**Calculations:**
- Days Overdue = Current Date - (Disbursement Date + Term Months)
- For each loan, apply provision percentage based on days overdue
- Total Provision = sum of all provisions

**SASRA Matrix:**
```
Days Overdue    Provision %
0               1%
1-90            25%
91-365          50%
365+            100%
```

**Filters Available:**
- As at Date

**Use Case:** Regulatory compliance, loan loss estimation

---

#### Report 13: SASRA COMPLIANCE REPORT
**Data Used:** All of the above combined

**What It Shows:**
- PAR 30 Ratio and compliance
- PAR 90 Ratio and compliance
- Core Capital Ratio and compliance
- Institutional Capital Ratio and compliance
- Liquidity Ratio (Liquid Assets / Total Liabilities) and compliance
- Savings to Loans Ratio and compliance
- Overall Compliance Status (COMPLIANT/NON-COMPLIANT)

**SASRA Compliance Checklist:**
```
✓ PAR 30 < 5%
✓ PAR 90 < 2%
✓ Core Capital >= 10%
✓ Institutional Capital >= 8%
✓ Liquidity Ratio >= 20%
✓ Savings to Loans >= 1.0
```

**Filters Available:**
- As at Date

**Use Case:** Regulatory compliance, SASRA submission

---

## PART 3: DATA FLOW DIAGRAM

```
SYSTEM DATA CAPTURE
│
├─ MEMBER DATA (member table)
│  └─ Used in: All reports (for member identification)
│
├─ ACCOUNT DATA (account table)
│  ├─ Used in: Trial Balance, Balance Sheet, Capital Adequacy
│  └─ Linked to: Member
│
├─ TRANSACTION DATA (transaction table)
│  ├─ Used in: Cashbook, Member Statement, P&L, Account Statement
│  └─ Linked to: Account → Member
│
├─ LOAN DATA (loan table)
│  ├─ Used in: Loan Register, Balance Sheet, PAR, Capital Adequacy, Provisions
│  └─ Linked to: Member, Loan Product
│
├─ LOAN REPAYMENT DATA (loan_repayment table)
│  ├─ Used in: Loan Register, Loan Statement
│  └─ Linked to: Loan
│
├─ GUARANTOR DATA (guarantor table)
│  ├─ Used in: Loan Register (optional)
│  └─ Linked to: Loan, Member
│
└─ AUDIT LOG DATA (audit_log table)
   ├─ Used in: Audit Trail Report
   └─ Linked to: User, Entity

REPORTS GENERATED
│
├─ TRANSACTION REPORTS
│  ├─ Cashbook (from transaction)
│  ├─ Member Statement (from transaction + account)
│  ├─ Account Statement (from transaction)
│  └─ Transaction History (from transaction)
│
├─ ACCOUNT REPORTS
│  ├─ Trial Balance (from account + loan)
│  └─ Balance Sheet (from account + loan)
│
├─ LOAN REPORTS
│  ├─ Loan Register (from loan + loan_repayment)
│  └─ Loan Statement (from loan + loan_repayment)
│
├─ FINANCIAL REPORTS
│  └─ Profit & Loss (from transaction + loan)
│
└─ COMPLIANCE REPORTS
   ├─ PAR Report (from loan)
   ├─ Capital Adequacy (from account + loan)
   ├─ Provision for Bad Debts (from loan)
   └─ SASRA Compliance (from all)
```

---

## PART 4: WHAT THE SYSTEM DOES NOT CAPTURE

### Missing Data That Would Enable Additional Reports:

1. **Expense Tracking**
   - No dedicated expense table
   - Expenses tracked via transaction descriptions (keyword-based)
   - **Impact:** P&L report depends on transaction keywords

2. **Interest Accrual**
   - Interest calculated at disbursement, not accrued monthly
   - **Impact:** Cannot generate monthly interest accrual reports

3. **Member Demographics**
   - No age, gender, employment details captured
   - **Impact:** Cannot generate demographic analysis reports

4. **Loan Collateral**
   - No collateral tracking
   - **Impact:** Cannot generate collateral valuation reports

5. **Member Attendance**
   - No meeting attendance tracking
   - **Impact:** Cannot generate member participation reports

6. **Dividend/Bonus Payments**
   - No dividend tracking
   - **Impact:** Cannot generate dividend distribution reports

7. **Expense Categories**
   - No expense categorization
   - **Impact:** Cannot generate detailed expense breakdown reports

8. **Budget vs Actual**
   - No budget data captured
   - **Impact:** Cannot generate budget variance reports

---

## PART 5: REPORT CAPABILITY SUMMARY

### Reports That Work Well (Data Fully Captured)
✓ Cashbook - All transaction data available
✓ Trial Balance - All account and loan data available
✓ Balance Sheet - All account and loan data available
✓ Member Statement - All transaction and account data available
✓ Loan Register - All loan and repayment data available
✓ PAR Report - All loan data available
✓ Capital Adequacy - All account and loan data available
✓ Provision for Bad Debts - All loan data available
✓ SASRA Compliance - All data available
✓ Audit Trail - All audit log data available

### Reports That Work With Limitations (Data Partially Captured)
⚠ Profit & Loss - Depends on transaction description keywords
  - If transactions don't have keywords like "salary", "rent", "loan fee", report shows zero
  - Requires discipline in transaction description entry

### Reports That Cannot Be Generated (Data Not Captured)
✗ Expense Breakdown - No expense categorization
✗ Member Demographics - No demographic data
✗ Collateral Report - No collateral data
✗ Attendance Report - No attendance data
✗ Dividend Report - No dividend data
✗ Budget Variance - No budget data

---

## PART 6: HOW TO ENSURE REPORTS WORK CORRECTLY

### For Cashbook Report
- Ensure all transactions are recorded in the system
- Use consistent transaction types
- Verify transaction dates are correct

### For Trial Balance & Balance Sheet
- Ensure all member accounts are created
- Verify account balances are accurate
- Ensure loans are properly recorded with outstanding balances

### For Loan Register
- Ensure all loans are recorded with correct details
- Verify loan status is updated correctly
- Record all loan repayments

### For PAR Report
- Ensure loan disbursement dates are correct
- Ensure loan term months are correct
- Update loan status to DEFAULTED for overdue loans

### For Capital Adequacy
- Ensure member shares are recorded in SHARES accounts
- Ensure member savings are recorded in SAVINGS accounts
- Verify account balances are accurate

### For Profit & Loss Report
- Use consistent keywords in transaction descriptions:
  - "salary" for salary expenses
  - "rent" for rent expenses
  - "utilities" for utility expenses
  - "loan fee" for loan processing fees
  - "account maintenance" for account fees
  - "fee" for other fees
  - "miscellaneous income" for other income
  - "miscellaneous expense" for other expenses
- Record loan defaults with status "DEFAULTED"

### For SASRA Compliance
- Ensure all above data is accurate
- Update loan status regularly
- Maintain accurate account balances

