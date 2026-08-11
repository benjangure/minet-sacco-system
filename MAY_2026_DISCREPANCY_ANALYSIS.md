# MAY 2026 DATA DISCREPANCY ANALYSIS
## Generated: August 10, 2026

---

## CRITICAL FINDINGS

### 🔴 **MAJOR ISSUE: Database doesn't match May 2026 CSV**

The database was restored from a dump file that contains **OLD DATA**, while the May 2026 CSV contains the **CORRECT CURRENT DATA** for May 2026.

---

## DATABASE vs CSV COMPARISON

### Sample Members Analysis:

| Employee ID | Name | Database Shares | CSV Shares (May C/F) | Difference | Database Loan | CSV Loan (May C/D) | Difference |
|-------------|------|----------------|---------------------|-----------|---------------|-------------------|-----------|
| **1087** | Fredrick Maina Mburu | KES 3,000 | KES 1,749,042 | **-1,746,042** ❌ | KES 1,756,850 | KES 1,931,300 | **-174,450** ❌ |
| **1191** | David Chege Waithaka | KES 3,000 | KES 8,760,000 | **-8,757,000** ❌ | NULL | NULL | ✅ |
| **1242** | Gabriel Mahugu Nduthu | KES 3,000 | KES 1,788,000 | **-1,785,000** ❌ | KES 4,578,365 | KES 4,654,670 | **-76,305** ❌ |
| **2054** | John Otieno Gangla | KES 3,000 | KES 1,190,000 | **-1,187,000** ❌ | KES 454,101 | KES 468,750 | **-14,649** ❌ |
| **4044** | Sammy Muthui | KES 3,000 | KES 5,000,000 | **-4,997,000** ❌ | KES 4,719,988 | KES 4,916,655 | **-196,667** ❌ |

---

## WHY THIS HAPPENED

### Root Cause:
The database was **restored from an OLD backup/dump** that:
1. Sets all shares accounts to **KES 3,000** (the mandatory share capital default)
2. Contains loan balances from a **previous period** (not May 2026)
3. **Does NOT contain** the progressive monthly data from January-May 2026

### What Should Be in Database (May 2026):

According to the CSV **totals at the bottom**:

```
EXPECTED (May 2026 End):
- Shares Balance C/F: KES 163,137,156
- Normal Loans C/D:   KES 64,228,784
- Emergency 1 C/D:    KES 179,917
- Guarantors:         KES 546,440

Monthly Contributions (May):
- Shares deposits (I):     KES 2,772,700
- Loan Principal paid (P): KES 2,053,526
- Loan Interest paid (I):  KES 662,823
```

### What's Actually in Database:

```
CURRENT DATABASE:
- Total Shares:        KES 642,000 (only 214 accounts)
- Active Loans:        KES 68,790,805
- Most shares = 3,000  (the default, not real data!)
```

---

## DETAILED DISCREPANCIES BY MEMBER

### Members with ZERO balances in database but have balances in CSV:

**Example members who should have significant balances:**

1. **Employee 9080 (MACHARIA EDWIN MWANGI)**
   - Database: Shares = ?, Loan = ?
   - CSV May: No data (marked with dashes)
   - **Status**: Appears to have no activity in May ✅

2. **Employee 13026 (BUNYALI JULIUS HABAKKUK)**
   - CSV May: No shares or loan data
   - **Status**: Inactive member ✅

3. **Employee 1312 (MAKAU LYDIA RUGURU)**
   - CSV May: No data
   - **Status**: Inactive ✅

### Members with INCORRECT balances:

**ALL ACTIVE MEMBERS** have incorrect balances because:
- Shares are set to 3,000 (default) instead of actual May C/F values
- Loan balances are from old data, not May 2026 C/D values

---

## WHAT THE CSV COLUMNS MEAN (CORRECT INTERPRETATION)

### Shares:
- **B/F** (Balance Brought Forward) = Opening balance from April 2026
- **I** (Interest/Deposits) = Amount deposited in May 2026
- **C/F** (Closing/Carried Forward) = **THIS SHOULD BE IN DATABASE** = B/F + I

### Normal Loan:
- **B/F** = Outstanding at start of May
- **P** = Principal paid in May (**should be recorded as repayment**)
- **I** = Interest paid in May (**should be recorded as repayment**)
- **C/D** (Closing/Carried Down) = **THIS SHOULD BE IN DATABASE** = B/F - P

### Emergency Loans 1 & 2:
- Same structure as Normal Loan

### Guarantors:
- **B/F** = Outstanding guarantor obligation start of May
- **P** = Paid in May
- **C/D** = **THIS SHOULD BE IN DATABASE** = B/F - P

---

## MATHEMATICAL VALIDATION

### Test Case: Employee 1087 (Fredrick Maina Mburu)

**From CSV:**
```
Shares:
  B/F:  1,739,042
  I:       10,000
  C/F:  1,749,042  ← Should be in database
  Calculation: 1,739,042 + 10,000 = 1,749,042 ✅

Normal Loan:
  B/F:  2,105,750
  P:      174,450  ← Principal repayment
  I:       21,058  ← Interest payment
  C/D:  1,931,300  ← Should be in database
  Calculation: 2,105,750 - 174,450 = 1,931,300 ✅
```

**In Database:**
```
Shares: 3,000         ❌ Should be 1,749,042
Loan:   1,756,850     ❌ Should be 1,931,300
```

---

## TRANSACTION TRACKING

### What's MISSING in Database:

For **EACH MONTH** (Jan, Feb, Mar, Apr, May), we should have:

1. **Shares deposits** (I column) recorded as transactions
2. **Loan principal payments** (P column) recorded as loan_repayments
3. **Interest payments** (I column) recorded as loan_repayments
4. **Updated balances** to match C/F and C/D columns

### Currently:
- **448 loan repayments** exist
- **2,165 transactions** exist
- But they don't match the CSV data!

---

## RECOMMENDATIONS

### ✅ **SOLUTION: Start Fresh Import**

1. **Clear all financial data** (keep members, users, settings)
2. **Import January 2026** (opening balances):
   - Shares: B/F - 3000
   - Loans: C/D
3. **Import February-May 2026 progressively**:
   - Record P + I as repayment transactions
   - Update shares with deposits (I)
   - Update balances to C/D and C/F

### 📊 **Expected Final Result (May 2026)**:

```sql
-- Shares
SELECT COUNT(*), SUM(balance) FROM accounts 
WHERE account_type = 'SHARES' AND balance > 0;
-- Expected: ~170 accounts, Total: KES 163,137,156

-- Loans
SELECT COUNT(*), SUM(outstanding_balance) FROM loans 
WHERE status IN ('DISBURSED', 'ACTIVE') AND outstanding_balance > 0;
-- Expected: ~70 loans, Total: KES 64,228,784

-- Repayments (Jan-May = 5 months)
SELECT COUNT(*), SUM(principal_amount + interest_amount) FROM loan_repayments;
-- Expected: Hundreds of repayments, matching monthly P+I totals
```

---

## KEY INSIGHTS

### 🔍 **Why Database Shows 3,000 for Shares:**

The restored dump has this code in `MemberService.java` line 380:
```java
sharesAccount.setBalance(new BigDecimal("3000.00")); 
// Mandatory share capital for all members
```

This is the **default initial balance**, not the accumulated balance from monthly contributions!

### 📅 **Timeline of What Should Have Happened:**

```
January 2026 Opening → (B/F - 3000 for shares, C/D for loans)
  ↓
February contributions → Record P+I, add deposits, update to Feb C/D
  ↓
March contributions → Record P+I, add deposits, update to Mar C/D
  ↓
April contributions → Record P+I, add deposits, update to Apr C/D
  ↓
May contributions → Record P+I, add deposits, update to May C/D
  ↓
MAY END = CSV "Balance C/F" and "C/D" columns
```

---

## CONCLUSION

**The database contains OLD, INCORRECT data from a previous backup.**

**The CSV contains the CORRECT, CURRENT data for May 2026.**

**Action Required:** Import all months (Jan-May 2026) properly to replace the old data with correct balances and transaction history.

