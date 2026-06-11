# STANDARD CASHBOOK TEMPLATE FOR SACCO
## Minet SACCO System - Cashbook Format Reference

---

## WHAT IS A CASHBOOK?

A cashbook is a **chronological record of all cash movements** (receipts and payments) for a specific period. It serves as the:
- Primary source document for cash management
- Bridge between operational transactions and GL accounts
- Audit trail for all cash movements
- Input data for bank reconciliation

---

## SAMPLE CASHBOOK FORMAT (Standard SACCO)

### **MINET COOP SOCIETY SACCO SOCIETY LIMITED**
### **CASHBOOK**
### **For the Period: 1 January 2023 to 31 December 2023**

```
Date | Ref | Description | Particulars | Receipts (Dr.) | Payments (Cr.) | Balance
-----|-----|-------------|-------------|----------------|----------------|----------
     |     |             |             | Member Dep. | Bank Ch. |
     |     |             |             | Interest    | Loan Dis.|
-----|-----|-------------|-------------|----------------|----------------|----------

01-Jan | CB-001 | Opening Balance | Balance from previous period | - | - | 50,000,000
02-Jan | DEP-001 | Member Deposit | Emp101 (Salary Deduction) | 50,000 | - | 50,050,000
02-Jan | LN-001 | Loan Disbursement | Emp102 (Loan LN-001) | - | 100,000 | 49,950,000
03-Jan | REP-001 | Loan Repayment | Emp101 repayment | 10,000 | - | 49,960,000
03-Jan | BANK | Bank Charges | Monthly fee | - | 500 | 49,959,500
04-Jan | INT-001 | Interest Income | Accrued on loans | 5,000 | - | 49,964,500
       |        |                 |                    |        |        |
... (more transactions) ...
       |        |                 |                    |        |        |
31-Dec | TOTAL  | **MONTHLY TOTAL** | **DECEMBER 2023** | **X,XXX,XXX** | **X,XXX,XXX** | **X,XXX,XXX**
```

---

## DETAILED CASHBOOK COLUMNS

### **Column 1: Date**
- Transaction date (DD-Mon-YY or DD/MM/YYYY)
- Transactions listed chronologically
- Example: 02-Jan-2023 or 02/01/2023

### **Column 2: Reference Number**
- Unique transaction identifier
- Format: PREFIX-SEQUENCE
- Examples:
  - `DEP-0001` = Deposit #1
  - `LN-0001` = Loan Disbursement #1
  - `REP-0001` = Repayment #1
  - `BANK-001` = Bank transaction
  - `INT-0001` = Interest receipt
  - `CB-001` = Cashbook opening

### **Column 3: Description/Narration**
- Type of transaction in narrative form
- Examples:
  - "Member Deposit"
  - "Loan Disbursement"
  - "Loan Repayment"
  - "Bank Charges"
  - "Interest Income"
  - "Honoraria Payment"
  - "Insurance Premium"

### **Column 4: Particulars**
- Details about the transaction
- Who it's for, member number, loan number
- Examples:
  - "Emp101 - Salary Deduction"
  - "Loan LN-0512 - Normal Loan"
  - "CBA Call Account - Monthly Interest"
  - "SASRA Annual Fee"

### **Column 5: Receipts (Debit Column)**
- Cash coming IN to the SACCO
- Single-column or broken down by type:
  - Member Deposits
  - Loan Repayments
  - Interest Income
  - Entrance Fees
  - Other Income
- Example: 50,000 (member deposit)
- Example: 10,000 (loan repayment)

### **Column 6: Payments (Credit Column)**
- Cash going OUT from the SACCO
- Single-column or broken down by type:
  - Loan Disbursements
  - Bank Charges
  - Honoraria/Salaries
  - Insurance
  - SASRA Fees
  - Other Expenses
- Example: 100,000 (loan disbursement)
- Example: 500 (bank charges)

### **Column 7: Running Balance**
- **Cash Balance = Previous Balance + Receipts - Payments**
- Reconciles to bank statement
- Example calculations:
  - Start: 50,000,000
  - After deposit +50,000: 50,050,000
  - After loan -100,000: 49,950,000
  - After repayment +10,000: 49,960,000
  - After charges -500: 49,959,500

---

## SAMPLE DATA WITH AMOUNTS

### **MINET COOP SOCIETY SACCO - CASHBOOK (Jan 2023)**

| Date | Ref | Description | Particulars | Receipts | Payments | Balance |
|------|-----|-------------|-------------|----------|----------|---------|
| 31-Dec-22 | OPEN | Opening Balance | Balance from 2022 | - | - | 86,000,000 |
| 02-Jan | DEP-001 | Member Deposit | Emp001 - Salary Deduction | 50,000 | - | 86,050,000 |
| 02-Jan | DEP-002 | Member Deposit | Emp002 - Salary Deduction | 75,000 | - | 86,125,000 |
| 02-Jan | LN-0512 | Loan Disbursement | Emp003 - Loan LN-0512 (Normal) | - | 500,000 | 85,625,000 |
| 03-Jan | REP-0315 | Loan Repayment | Emp004 - Repayment on LN-0315 | 12,500 | - | 85,637,500 |
| 03-Jan | BANK-001 | Bank Charges | CBA Monthly Fee | - | 2,000 | 85,635,500 |
| 04-Jan | INT-001 | Interest Income | Interest on Call Deposits | 15,000 | - | 85,650,500 |
| 04-Jan | LN-0513 | Loan Disbursement | Emp005 - Loan LN-0513 (Emergency) | - | 50,000 | 85,600,500 |
| 05-Jan | DEP-003 | Member Deposit | Emp006 - Monthly Contribution | 30,000 | - | 85,630,500 |
| 05-Jan | PAY-001 | Committee Allowance | January Committee Meeting | - | 10,000 | 85,620,500 |
| 06-Jan | REP-0316 | Loan Repayment | Emp007 - Repayment on LN-0316 | 25,000 | - | 85,645,500 |
| 06-Jan | INS-001 | Insurance Premium | Monthly Building Insurance | - | 5,000 | 85,640,500 |
| 07-Jan | LN-0514 | Loan Disbursement | Emp008 - Loan LN-0514 (Normal) | - | 750,000 | 84,890,500 |
| 08-Jan | DEP-004 | Member Deposit | Emp009 - Salary Deduction | 60,000 | - | 84,950,500 |
| 08-Jan | REP-0317 | Loan Repayment | Emp010 - Repayment on LN-0317 | 18,000 | - | 84,968,500 |
| **10-Jan** | **JAN-TOTAL** | **SUBTOTAL FOR JANUARY** | **First 10 Days** | **375,500** | **1,317,000** | **84,968,500** |
| ... | ... | (continue through month) | ... | ... | ... | ... |
| 31-Jan | MON-TOTAL | **MONTHLY TOTAL** | **JANUARY 2023** | **2,456,200** | **2,987,500** | **85,468,700** |

---

## TYPICAL RECEIPT CATEGORIES (Receipts Column)

```
RECEIPTS (Money Coming In):
├── Member Deposits (Salary Deduction)         50,000 + 75,000 + ...
├── Member Savings (Voluntary)                 30,000 + ...
├── Loan Repayments                            12,500 + 25,000 + 18,000 + ...
├── Interest Income
│   ├── Interest on Loans                      5,000 + ...
│   ├── Interest on Call Deposits              15,000 + ...
│   └── Other Interest                         2,500 + ...
├── Entrance Fees (New Members)                500 + 500 + ...
├── Fines & Penalties                          200 + ...
├── Miscellaneous Income                       1,000 + ...
└── TOTAL RECEIPTS (Period)                    2,456,200
```

---

## TYPICAL PAYMENT CATEGORIES (Payments Column)

```
PAYMENTS (Money Going Out):
├── Loan Disbursements
│   ├── Normal Loans                           500,000 + 750,000 + ...
│   ├── Emergency Loans                        50,000 + ...
│   └── Other Loans                            ...
├── Personnel Costs
│   ├── Committee Honoraria                    10,000 + ...
│   ├── Staff Salaries                         (if applicable)
│   └── Other Allowances                       5,000 + ...
├── Operating Expenses
│   ├── Bank Charges                           2,000 + ...
│   ├── Insurance Premiums                     5,000 + ...
│   ├── Office Rent                            20,000 + ...
│   ├── Utilities                              2,500 + ...
│   └── Supplies                               1,500 + ...
├── Regulatory Compliance
│   ├── SASRA Fees & Registration              ...
│   ├── Audit Fees                             ...
│   └── AGM Expenses                           ...
├── Loan Impairment/Write-offs                 ...
├── Miscellaneous Payments                     1,000 + ...
└── TOTAL PAYMENTS (Period)                    2,987,500
```

---

## KEY METRICS TO EXTRACT FROM CASHBOOK

After completing the cashbook, calculate:

1. **Total Receipts (Period)**
   - Sum of all receipt entries
   - Example: KES 2,456,200

2. **Total Payments (Period)**
   - Sum of all payment entries
   - Example: KES 2,987,500

3. **Net Cash Flow**
   - Receipts - Payments
   - Example: 2,456,200 - 2,987,500 = (531,300) [negative = net outflow]

4. **Opening Balance**
   - Cash at start of period
   - Example: KES 86,000,000

5. **Closing Balance**
   - Opening + Net Cash Flow
   - Example: 86,000,000 - 531,300 = KES 85,468,700

6. **Reconciliation to Bank Statement**
   - Closing Balance (Cashbook) = Bank Balance (Statement)
   - Account for: Cheques not yet cleared, deposits not yet credited, etc.

---

## HOW THIS MAPS TO YOUR SYSTEM

### **Current System Data Structure (Transactions Table)**

```sql
SELECT 
  transaction_date AS Date,
  'REF-' || id AS Reference,
  CASE 
    WHEN transaction_type = 'DEPOSIT' THEN 'Member Deposit'
    WHEN transaction_type = 'WITHDRAWAL' THEN 'Member Withdrawal'
    WHEN transaction_type = 'LOAN_DISBURSEMENT' THEN 'Loan Disbursement'
    WHEN transaction_type = 'LOAN_REPAYMENT' THEN 'Loan Repayment'
    WHEN transaction_type = 'INTEREST' THEN 'Interest Income'
  END AS Description,
  account_member_id || ' - ' || description AS Particulars,
  CASE WHEN transaction_type IN ('DEPOSIT', 'LOAN_REPAYMENT', 'INTEREST') 
       THEN amount ELSE 0 END AS Receipts,
  CASE WHEN transaction_type IN ('WITHDRAWAL', 'LOAN_DISBURSEMENT') 
       THEN amount ELSE 0 END AS Payments,
  (SELECT SUM(amount) FROM transactions t2 
   WHERE t2.transaction_type IN ('DEPOSIT','LOAN_REPAYMENT','INTEREST')
   AND t2.transaction_date <= t1.transaction_date) -
  (SELECT SUM(amount) FROM transactions t2 
   WHERE t2.transaction_type IN ('WITHDRAWAL','LOAN_DISBURSEMENT')
   AND t2.transaction_date <= t1.transaction_date) AS Balance
FROM transactions t1
ORDER BY transaction_date;
```

---

## WHAT WE NEED FROM THE MEETING

**Ask for their official cashbook template that includes:**

1. ✅ Their exact column headers (Date, Ref, Description, etc.)
2. ✅ Their receipt categories breakdown (is it single column or multi-column?)
3. ✅ Their payment categories breakdown
4. ✅ Their reference number format (DEP-0001, or something else?)
5. ✅ Their monthly summary format
6. ✅ Any special entries they use (opening balance, transfers between accounts, etc.)
7. ✅ Sample data showing their actual format with real amounts

---

## NEXT STEPS AFTER GETTING THEIR TEMPLATE

Once they provide their official cashbook template:

1. **Map Their Format to System Data**
   - Align our transaction types to their categories
   - Adjust reference number generation
   - Verify receipt/payment breakdown

2. **Configure ReportsService**
   - Modify `generateCashbook()` method to match their format
   - Ensure date ranges and summaries align
   - Add any missing transaction types

3. **Generate Test Cashbook**
   - Run system against real data
   - Compare to manually prepared cashbook
   - Verify all transactions captured
   - Validate running balance

4. **Build Frontend Report UI**
   - Create cashbook export (PDF, Excel)
   - Add filters (date range, transaction type)
   - Show summary totals

---

## SACCO CASHBOOK BEST PRACTICES

1. **Daily Entry**: Record all transactions same day
2. **Supporting Documents**: Every entry has receipt/voucher
3. **Bank Reconciliation**: Monthly comparison with bank statement
4. **Authorization**: Cashier signs off, Manager verifies
5. **Audit Trail**: Reference numbers never repeated
6. **Running Balance**: Verify daily to catch errors early
7. **Segregation**: Member deposits separate from operational cash
8. **Backup Accounts**: Multiple signatories for large amounts

---

## COMMON CHALLENGES & SOLUTIONS

| Challenge | Solution |
|-----------|----------|
| Missing transactions | Daily reconciliation with member deposits |
| Uncleared cheques | Create separate "In Transit" section |
| Inter-account transfers | Create "Contra" entries (receipt + payment same amount) |
| Rounding differences | Reconcile to nearest shilling |
| Late-posted transactions | Use transaction date, not posting date |
| Manual cash vs Bank | Daily manual cash count vs bank statement |

---

**Status:** Ready for their cashbook template. Once received, we can finalize the system configuration.
