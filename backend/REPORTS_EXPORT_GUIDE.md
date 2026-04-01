# Reports Export Guide - Excel & PDF with Filters

## Overview

All financial reports can now be exported in both **Excel (.xlsx)** and **PDF (.txt)** formats with comprehensive filtering options.

---

## Available Reports

### 1. CASHBOOK REPORT
**Purpose**: Daily transaction log for cash management

**Filters**:
- `startDate` (required): Start date (YYYY-MM-DD)
- `endDate` (required): End date (YYYY-MM-DD)
- `memberNumber` (optional): Filter by specific member
- `transactionType` (optional): DEPOSIT, WITHDRAWAL, LOAN_DISBURSEMENT, LOAN_REPAYMENT, INTEREST
- `accountType` (optional): SAVINGS, SHARES, BENEVOLENT_FUND, DEVELOPMENT_FUND, SCHOOL_FEES, HOLIDAY_FUND, EMERGENCY_FUND

**Endpoints**:
```
GET /api/reports/cashbook?startDate=2024-01-01&endDate=2024-01-31&memberNumber=MNT-00001&transactionType=DEPOSIT
GET /api/reports/cashbook/export/excel?startDate=2024-01-01&endDate=2024-01-31
GET /api/reports/cashbook/export/pdf?startDate=2024-01-01&endDate=2024-01-31
```

**Contains**:
- Summary: Total deposits, withdrawals, repayments, net cash
- Transaction details: Date, type, member, account, amount, description

---

### 2. TRIAL BALANCE REPORT
**Purpose**: Verify accounting equation (debits = credits)

**Filters**:
- `memberNumber` (optional): Filter by specific member
- `accountType` (optional): SAVINGS, SHARES, LOAN, etc.

**Endpoints**:
```
GET /api/reports/trial-balance?memberNumber=MNT-00001
GET /api/reports/trial-balance/export/excel
GET /api/reports/trial-balance/export/pdf?accountType=SAVINGS
```

**Contains**:
- All accounts with debit/credit balances
- Member accounts as liabilities (credits)
- Loans as assets (debits)
- Balance verification status

---

### 3. BALANCE SHEET REPORT
**Purpose**: Financial position snapshot (Assets, Liabilities, Equity)

**Filters**: None (shows entire organization)

**Endpoints**:
```
GET /api/reports/balance-sheet
GET /api/reports/balance-sheet/export/excel
GET /api/reports/balance-sheet/export/pdf
```

**Contains**:
- Assets: Total loans outstanding
- Liabilities: Member savings + shares
- Equity: Assets - Liabilities

---

### 4. MEMBER STATEMENT REPORT
**Purpose**: Individual member account history

**Filters**:
- `memberId` (required): Member ID
- `startDate` (required): Start date (YYYY-MM-DD)
- `endDate` (required): End date (YYYY-MM-DD)

**Endpoints**:
```
GET /api/reports/member-statement/1?startDate=2024-01-01&endDate=2024-01-31
GET /api/reports/member-statement/1/export/excel?startDate=2024-01-01&endDate=2024-01-31
GET /api/reports/member-statement/1/export/pdf?startDate=2024-01-01&endDate=2024-01-31
```

**Contains**:
- Transaction history by date
- Current balances by account type
- Total deposits and withdrawals

---

### 5. LOAN REGISTER REPORT
**Purpose**: Complete loan portfolio overview

**Filters**:
- `memberNumber` (optional): Filter by member
- `loanStatus` (optional): PENDING, APPROVED, REJECTED, DISBURSED, REPAID, DEFAULTED
- `loanProduct` (optional): Filter by loan product name

**Endpoints**:
```
GET /api/reports/loan-register?loanStatus=DISBURSED
GET /api/reports/loan-register/export/excel?memberNumber=MNT-00001
GET /api/reports/loan-register/export/pdf?loanProduct=Personal
```

**Contains**:
- All loans with details: amount, interest rate, term, monthly payment
- Loan status and outstanding balance
- Totals: issued, outstanding, repaid

---

## Export Formats

### Excel Format (.xlsx)
- Professional spreadsheet format
- Auto-sized columns
- Summary sections
- Easy to manipulate and analyze
- Compatible with Excel, Google Sheets, LibreOffice

### PDF Format (.txt)
- Formatted text file (plain text)
- Professional layout with headers and sections
- Fixed-width columns for alignment
- Can be opened in any text editor
- Easy to print or email

---

## Usage Examples

### Example 1: Export Cashbook for January 2024
```bash
curl -X GET "http://localhost:8080/api/reports/cashbook/export/excel?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o cashbook_january.xlsx
```

### Example 2: Export Trial Balance for specific member
```bash
curl -X GET "http://localhost:8080/api/reports/trial-balance/export/pdf?memberNumber=MNT-00001" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o trial_balance_member.txt
```

### Example 3: Export Loan Register for DISBURSED loans
```bash
curl -X GET "http://localhost:8080/api/reports/loan-register/export/excel?loanStatus=DISBURSED" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o loan_register_disbursed.xlsx
```

### Example 4: Export Member Statement
```bash
curl -X GET "http://localhost:8080/api/reports/member-statement/1/export/excel?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o member_statement_MNT-00001.xlsx
```

---

## Filter Combinations

### Cashbook Filters
- **By Date Range**: `startDate` + `endDate` (required)
- **By Member**: Add `memberNumber=MNT-00001`
- **By Transaction Type**: Add `transactionType=DEPOSIT`
- **By Account**: Add `accountType=SAVINGS`
- **Combined**: All filters can be combined

Example:
```
/api/reports/cashbook/export/excel?startDate=2024-01-01&endDate=2024-01-31&memberNumber=MNT-00001&transactionType=DEPOSIT&accountType=SAVINGS
```

### Trial Balance Filters
- **By Member**: `memberNumber=MNT-00001`
- **By Account Type**: `accountType=SAVINGS`
- **Combined**: Both can be used together

Example:
```
/api/reports/trial-balance/export/excel?memberNumber=MNT-00001&accountType=SAVINGS
```

### Loan Register Filters
- **By Member**: `memberNumber=MNT-00001`
- **By Status**: `loanStatus=DISBURSED`
- **By Product**: `loanProduct=Personal`
- **Combined**: All filters can be combined

Example:
```
/api/reports/loan-register/export/pdf?memberNumber=MNT-00001&loanStatus=DISBURSED&loanProduct=Personal
```

---

## Role-Based Access

### Cashbook, Trial Balance, Balance Sheet
- ADMIN
- TREASURER
- AUDITOR

### Member Statement
- ADMIN
- TREASURER
- LOAN_OFFICER
- CUSTOMER_SUPPORT
- AUDITOR

### Loan Register
- ADMIN
- TREASURER
- LOAN_OFFICER
- AUDITOR

---

## File Naming Convention

Files are automatically named with the report type and date:
- `cashbook_2024-03-10.xlsx`
- `trial_balance_2024-03-10.txt`
- `balance_sheet_2024-03-10.xlsx`
- `member_statement_MNT-00001_2024-03-10.xlsx`
- `loan_register_2024-03-10.txt`

---

## Report Contents

### Cashbook
```
CASHBOOK REPORT
===============

Period: 2024-01-01 to 2024-01-31
Generated: 2024-03-10 14:30:45

SUMMARY
-------
Total Deposits:     KES 500,000.00
Total Withdrawals:  KES 100,000.00
Total Repayments:   KES 50,000.00
Net Cash:           KES 450,000.00

TRANSACTIONS
------------
Date         Type                 Member       Account         Amount          Description
2024-01-01   DEPOSIT              MNT-00001    SAVINGS         10,000.00       Monthly contribution
...
```

### Trial Balance
```
TRIAL BALANCE REPORT
====================

Generated: 2024-03-10 14:30:45

Member          Account Type         Debit                Credit
MNT-00001       SAVINGS              0.00                 50,000.00
MNT-00001       LOAN                 25,000.00            0.00
...
TOTALS                               25,000.00            50,000.00

Balanced: YES
```

### Balance Sheet
```
BALANCE SHEET
=============

As at: 2024-03-10

ASSETS
------
Loans Outstanding:  KES 500,000.00

LIABILITIES
-----------
Member Savings:     KES 300,000.00
Member Shares:      KES 150,000.00
Total Liabilities:  KES 450,000.00

EQUITY
------
Equity:             KES 50,000.00
```

---

## Tips for Using Reports

1. **Compliance**: Generate monthly reports for regulatory compliance
2. **Analysis**: Use filters to analyze specific members or transaction types
3. **Audit**: Use Trial Balance to verify accounting accuracy
4. **Statements**: Generate member statements on request
5. **Portfolio**: Use Loan Register to monitor loan portfolio health
6. **Archiving**: Save reports in Excel for long-term storage and analysis

---

## Troubleshooting

**Issue**: Report shows no data
- **Solution**: Check date range, ensure transactions exist in that period

**Issue**: Filter not working
- **Solution**: Verify filter parameter names and values are correct

**Issue**: File download fails
- **Solution**: Check user role has permission to access report

**Issue**: Large report takes time to generate
- **Solution**: Use filters to narrow down data range

---

## Future Enhancements

- PDF generation with iText library (currently using text format)
- CSV export format
- Scheduled report generation
- Email report delivery
- Report templates customization
- Multi-currency support

