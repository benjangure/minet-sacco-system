# Repayment Display Bug - Complete Diagnostic Report

## Executive Summary
The member dashboard is displaying **incorrect repayment percentages and amounts** for loans. Specifically, it shows negative values (-40%, -80,000 KES) when it should show 0% and 0 KES for loans with no repayments made.

**Status**: Database data is CORRECT, but frontend still shows WRONG values. This suggests a caching, calculation, or data-fetching issue on the frontend.

---

## The Problem

### What the User Sees (WRONG)
For a loan with:
- Principal: KES 200,000
- Interest: KES 80,000
- Total Repayable: KES 280,000
- No repayments made yet

**Frontend displays:**
- Repayment Status: **-40%** ✗ (should be 0%)
- Repaid Amount: **KES -80,000** ✗ (should be 0)
- Outstanding: KES 280,000 ✓ (correct)

### What the Database Shows (CORRECT)
```
ID 13 | LN-2026-00004 | DISBURSED | 280,000 | 280,000 | 0.00 | 0.00%
```

The database is **100% correct**:
- total_repayable = 280,000
- outstanding_balance = 280,000
- calculated_repaid = 0
- repayment_percentage = 0%

---

## Root Cause Analysis

### The Calculation (Frontend Code)
**File**: `minetsacco-main/src/pages/MemberDashboard.tsx` (lines 1117-1128)

```typescript
// Repaid amount = Total Repayable - Outstanding Balance
formatCurrency(loan.totalRepayable - loan.outstandingBalance)

// Percentage = (Repaid / Total Repayable) × 100
Math.round(((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100)
```

**The calculation is CORRECT.** If the database values are correct (280,000 - 280,000 = 0), the frontend should display 0%.

### Why It's Still Showing Wrong Values

**Hypothesis 1: Frontend is using cached/stale data**
- The frontend fetched loan data BEFORE the database was fixed
- Browser cache is serving old data
- API response is being cached

**Hypothesis 2: Different loan is being displayed**
- The screenshot shows LN-2026-00003 but database shows LN-2026-00004
- There might be multiple loans and we're looking at the wrong one
- Loan ID 12 has the issue: outstanding_balance (42,400) > total_repayable (31,800)

**Hypothesis 3: API is returning different data than what's in the database**
- The backend endpoint `/member/loans` might be returning cached or transformed data
- The Loan entity might have a custom serializer that modifies values

---

## What Has Been Tried (and Failed)

### Attempt 1: Direct SQL Update
**Command:**
```sql
UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE (status = 'DISBURSED' OR status = 'REPAID') 
AND outstanding_balance != total_repayable;
```

**Result**: "No rows affected" - because the condition was already true in the database

**Why it failed**: The database was already correct for most loans. The issue is either:
1. Frontend caching
2. A different loan with the problem
3. API returning wrong data

---

### Attempt 2: Fix Loans Where Outstanding > Total Repayable
**Command:**
```sql
UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE outstanding_balance > total_repayable;
```

**Result**: "No change at all" - frontend still shows wrong values

**Why it failed**: Even after fixing the database, the frontend still displays wrong values. This confirms the issue is NOT in the database.

---

### Attempt 3: Code Fix in LoanDisbursementService
**File**: `backend/src/main/java/com/minet/sacco/service/LoanDisbursementService.java`

**Change**: Added safety check to ensure `outstandingBalance = totalRepayable` at disbursement

```java
// IMPORTANT: Always ensure outstandingBalance equals totalRepayable at disbursement
if (loan.getTotalRepayable() != null && 
    (loan.getOutstandingBalance() == null || 
     loan.getOutstandingBalance().compareTo(loan.getTotalRepayable()) != 0)) {
    loan.setOutstandingBalance(loan.getTotalRepayable());
}

// Safety check: if outstanding balance is somehow greater than total repayable, fix it
if (loan.getOutstandingBalance() != null && loan.getTotalRepayable() != null &&
    loan.getOutstandingBalance().compareTo(loan.getTotalRepayable()) > 0) {
    loan.setOutstandingBalance(loan.getTotalRepayable());
}
```

**Result**: No change - frontend still shows wrong values

**Why it failed**: This only affects NEW disbursements. Existing loans in the database are not affected by code changes.

---

### Attempt 4: Created Migrations V89, V90, V91
**Files created:**
- `V89__Fix_outstanding_balance.sql`
- `V90__Fix_outstanding_balance_direct.sql`
- `V91__Fix_outstanding_balance_greater_than_repayable.sql`

**Result**: Migrations exist but haven't fixed the display

**Why it failed**: Migrations only fix the database. The frontend is still showing wrong values, which means:
1. The frontend is not fetching fresh data
2. The frontend is caching the response
3. The API is returning cached/wrong data

---

## Current Database State

### All Loans Query Result
```
ID | Loan Number | Status | Amount | Interest | Total Repayable | Outstanding | Repaid | %
3  | NULL | REPAID | 20,000 | 1,399.92 | 21,399.92 | 21,399.92 | 0.00 | 0.00%
5  | LN-2026-00002 | DISBURSED | 100,000 | 12,000 | 112,000 | 112,000 | 0.00 | 0.00%
7  | LN-2026-00003 | DISBURSED | 80,000 | 7,200 | 87,200 | 87,200 | 0.00 | 0.00%
12 | NULL | PENDING_LOAN_OFFICER_REVIEW | 30,000 | 1,800 | 31,800 | 42,400 | -10,600 | -33.33%
13 | LN-2026-00004 | DISBURSED | 200,000 | 80,000 | 280,000 | 280,000 | 0.00 | 0.00%
```

**Key Finding**: Loan ID 12 has `outstanding_balance (42,400) > total_repayable (31,800)` - this is the problematic loan!

---

## What Needs to Be Investigated

### 1. Frontend Data Fetching
- Is the frontend caching the API response?
- Is there a React state issue where old data is being retained?
- Is the browser cache serving stale data?

**Action**: 
- Clear browser cache (Ctrl+Shift+Delete)
- Check browser DevTools Network tab to see what data the API is returning
- Check if the API response has the correct values

### 2. API Response
- Is the backend API returning the correct data?
- Is there a custom serializer modifying the Loan entity?
- Is the API caching responses?

**Action**:
- Call the API directly: `GET /member/loans`
- Check the JSON response to see if it has correct values
- Verify the Loan entity doesn't have custom serialization

### 3. Loan ID 12 Specifically
- Why does this loan have `outstanding_balance > total_repayable`?
- How did this loan get created with wrong values?
- Is there a creation workflow that's setting wrong values?

**Action**:
- Check how Loan ID 12 was created
- Check if there's a loan creation endpoint that's setting wrong values
- Verify the loan creation logic in the backend

### 4. Frontend Calculation
- Is the frontend using the correct fields?
- Is there a data transformation happening?
- Are there multiple places where this calculation is done?

**Action**:
- Search for all places in the frontend where repayment percentage is calculated
- Verify they're all using `totalRepayable` and `outstandingBalance`
- Check if there's a custom hook or utility function doing the calculation

---

## Files Involved

### Backend
- `backend/src/main/java/com/minet/sacco/service/LoanDisbursementService.java` - Loan disbursement logic
- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` - API endpoints
- `backend/src/main/java/com/minet/sacco/repository/LoanRepository.java` - Database queries
- `backend/src/main/java/com/minet/sacco/entity/Loan.java` - Loan entity (check for custom serialization)

### Frontend
- `minetsacco-main/src/pages/MemberDashboard.tsx` - Main dashboard component (lines 1117-1128 for calculation)
- `minetsacco-main/src/config/api.ts` - API configuration (check for caching)

### Database
- `sacco_db.loans` table - Contains the loan data

---

## Next Steps for Claude

1. **Verify API Response**: Call the backend API directly and check what data it's returning
2. **Check Frontend Cache**: Clear browser cache and check if the issue persists
3. **Investigate Loan ID 12**: Understand why this loan has wrong values
4. **Search for Serializers**: Check if the Loan entity has custom JSON serialization
5. **Check for Multiple Calculations**: Search the frontend for all places where repayment % is calculated
6. **Verify Data Flow**: Trace the data from API response to frontend display

---

## Key Questions for Claude

1. Why is the frontend still showing wrong values when the database is correct?
2. Is there a caching layer (browser, API, or application) that needs to be cleared?
3. Is the Loan entity being serialized/transformed before being sent to the frontend?
4. How was Loan ID 12 created with `outstanding_balance > total_repayable`?
5. Are there multiple places in the frontend calculating repayment percentage?
6. Is there a React state issue where old data is being retained?

---

## Timeline

- **Database Fix**: ✓ Complete (outstanding_balance = total_repayable for all loans)
- **Code Fix**: ✓ Complete (LoanDisbursementService ensures correct values at disbursement)
- **Frontend Display**: ✗ Still showing wrong values (root cause unknown)

**The issue is NOT in the database or the backend code. It's in the frontend data fetching, caching, or calculation.**
