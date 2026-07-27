# Technical Analysis - Repayment Display Bug

## Problem Statement

Frontend displays **-80,000 KES and -40%** for loan repayment progress when it should display **0 KES and 0%**.

**Loan Details:**
- Loan ID: 13
- Loan Number: LN-2026-00004
- Status: DISBURSED
- Principal Amount: 200,000 KES
- Interest Rate: 40% per annum
- Term: 12 months
- Total Interest: 80,000 KES
- Total Repayable: 280,000 KES
- Outstanding Balance: 280,000 KES (correct)
- Calculated Repaid: 0 KES (correct)
- Repayment Percentage: 0% (correct)

---

## The -80,000 Clue

The frontend displays exactly **-80,000 KES**, which is the **interest amount**.

This is not a coincidence. It means the frontend is doing one of these calculations:

### Calculation 1: Wrong Field Usage
```
Repaid = amount - totalRepayable
Repaid = 200,000 - 280,000 = -80,000 ✓
```

### Calculation 2: Wrong Outstanding Balance
```
Repaid = totalRepayable - (totalRepayable + interest)
Repaid = 280,000 - 360,000 = -80,000 ✓
```

### Calculation 3: Inverted Calculation
```
Repaid = outstandingBalance - totalRepayable
Repaid = 280,000 - 280,000 = 0 ✗ (This would show 0, not -80,000)
```

### Calculation 4: Using Interest as Negative
```
Repaid = -totalInterest
Repaid = -80,000 ✓
```

---

## Code Analysis

### Frontend Code (MemberDashboard.tsx, Lines 1117-1128)

**Current Code:**
```typescript
<div className="flex justify-between text-sm mb-1">
  <span className="text-muted-foreground">Progress</span>
  <span className="font-medium">
    {formatCurrency(loan.totalRepayable - loan.outstandingBalance)} / {formatCurrency(loan.totalRepayable)}
  </span>
</div>
<div className="w-full bg-gray-200 rounded-full h-2">
  <div 
    className="bg-green-600 h-2 rounded-full transition-all duration-300"
    style={{ width: `${Math.min(((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100, 100)}%` }}
  />
</div>
<div className="flex justify-between text-xs text-muted-foreground mt-1">
  <span>{Math.round(((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100)}% repaid</span>
  <span>{formatCurrency(loan.outstandingBalance)} remaining</span>
</div>
```

**Analysis:**
- Formula is mathematically correct: `totalRepayable - outstandingBalance`
- For our loan: `280,000 - 280,000 = 0` ✓
- But frontend is displaying `-80,000` ✗

**Conclusion:** Either:
1. The frontend is receiving different data than expected
2. The frontend is using a different field than what's shown in the code
3. There's a response interceptor modifying the data

---

### Backend Code (MemberPortalController.java, Line 254)

```java
@GetMapping("/loans")
public ResponseEntity<?> getLoans() {
    try {
        Member member = getCurrentMember();
        List<Loan> loans = loanRepository.findByMemberId(member.getId());
        return ResponseEntity.ok(loans);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
}
```

**Analysis:**
- Returns loans directly from repository
- No transformation or calculation
- No custom serialization
- Should return correct data

---

### Loan Entity (Loan.java)

**Key Fields:**
```java
@DecimalMin(value = "0.00")
private BigDecimal amount;

@DecimalMin(value = "0.00")
private BigDecimal totalInterest;

@DecimalMin(value = "0.00")
private BigDecimal totalRepayable;

@DecimalMin(value = "0.00")
private BigDecimal outstandingBalance;
```

**Analysis:**
- No @JsonSerialize annotations
- No @JsonProperty annotations
- No custom getters/setters that modify values
- Should serialize correctly

---

## Possible Root Causes

### Root Cause 1: API Response Interceptor

**File:** `minetsacco-main/src/config/api.ts`

```typescript
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

**Check:** Is there a response interceptor that modifies the loan data?

**Current Status:** Response interceptor is commented out (lines 28-42)

---

### Root Cause 2: Frontend Data Transformation

**File:** `minetsacco-main/src/pages/MemberDashboard.tsx`

**Check:** Is there any code that transforms the loan data after receiving it from the API?

**Current Status:** No transformation visible in the code

---

### Root Cause 3: Backend Sending Wrong Data

**Check:** Is the backend sending `outstandingBalance` with a different value?

**Current Status:** Database shows correct values, but need to verify API response

---

### Root Cause 4: Custom Deserializer

**Check:** Is there a custom Jackson deserializer that modifies the values?

**Current Status:** No custom deserializers found in the codebase

---

## Debug Strategy

### Step 1: Capture API Response

Add console.log in MemberDashboard.tsx (already done at line 1117):

```typescript
console.log(`[DEBUG] Loan ${loan.id} (${loan.loanNumber}) - Raw API Data:`, {
  id: loan.id,
  loanNumber: loan.loanNumber,
  status: loan.status,
  amount: loan.amount,
  totalInterest: loan.totalInterest,
  totalRepayable: loan.totalRepayable,
  outstandingBalance: loan.outstandingBalance,
  calculatedRepaid: loan.totalRepayable - loan.outstandingBalance,
  calculatedPercentage: ((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100
});
```

**Expected Output:**
```
[DEBUG] Loan 13 (LN-2026-00004) - Raw API Data: {
  id: 13,
  loanNumber: "LN-2026-00004",
  status: "DISBURSED",
  amount: 200000,
  totalInterest: 80000,
  totalRepayable: 280000,
  outstandingBalance: 280000,
  calculatedRepaid: 0,
  calculatedPercentage: 0
}
```

**If Different:** The frontend is receiving wrong data from the API

### Step 2: Capture Network Response

Check the Network tab for the `member/loans` request.

**Expected Response:**
```json
[
  {
    "id": 13,
    "loanNumber": "LN-2026-00004",
    "status": "DISBURSED",
    "amount": 200000,
    "totalInterest": 80000,
    "totalRepayable": 280000,
    "outstandingBalance": 280000,
    "monthlyRepayment": 23333.33,
    "repayments": []
  }
]
```

**If Different:** The backend is sending wrong data

### Step 3: Compare Console Output with Network Response

- If they match but display is wrong → Frontend calculation is wrong
- If they don't match → Response interceptor is modifying the data
- If network response is wrong → Backend is sending wrong data

---

## Fix Strategy

### If Backend is Sending Wrong Data

1. Check `LoanDisbursementService.disburseLoan()` to verify `outstandingBalance` is set correctly
2. Check if there's a custom query calculating `outstandingBalance` incorrectly
3. Create a migration to fix existing data if needed

### If Response Interceptor is Modifying Data

1. Check `minetsacco-main/src/config/api.ts` for response interceptors
2. Remove or fix any interceptors that modify loan data
3. Verify the response is not being transformed

### If Frontend Calculation is Wrong

1. Find where the calculation is happening
2. Check if it's using the correct fields
3. Fix the formula to use `totalRepayable - outstandingBalance`
4. Add `Math.max(0, ...)` guard to prevent negative values

### Regardless of Root Cause

Add safety guard to prevent negative display values:

```typescript
const repaidAmount = Math.max(0, loan.totalRepayable - loan.outstandingBalance);
const repaidPercentage = loan.totalRepayable > 0
  ? Math.max(0, Math.round((repaidAmount / loan.totalRepayable) * 100))
  : 0;
```

---

## Testing Plan

### Test 1: Verify Database

```sql
SELECT id, loan_number, status, amount, total_interest, total_repayable, 
       outstanding_balance, (total_repayable - outstanding_balance) as calculated_repaid
FROM loans
WHERE id = 13;
```

**Expected:**
```
id | loan_number | status | amount | total_interest | total_repayable | outstanding_balance | calculated_repaid
13 | LN-2026-00004 | DISBURSED | 200000 | 80000 | 280000 | 280000 | 0
```

### Test 2: Verify API Response

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/member/loans
```

**Expected:** JSON with `outstandingBalance: 280000`

### Test 3: Verify Frontend Display

1. Log in to Member Portal
2. Go to Loans page
3. Check console for [DEBUG] messages
4. Verify display shows 0 KES and 0%

---

## Key Insights

1. **The -80,000 is the interest amount** - This is the smoking gun. It means the frontend is using the wrong field or calculation.

2. **The database is correct** - All loans show `outstanding_balance = total_repayable` and `calculated_repaid = 0.00`.

3. **The backend API is correct** - The MemberPortalController returns loans directly from the repository without transformation.

4. **The frontend code looks correct** - The formula `totalRepayable - outstandingBalance` is mathematically correct.

5. **Something is wrong in the middle** - Either the API response is being intercepted and modified, or the frontend is receiving different data than expected.

---

## Next Steps

1. **Get the debug data** - Console output and network response
2. **Analyze the data** - Compare what the frontend receives vs. what it displays
3. **Identify the root cause** - Backend, API response, or frontend
4. **Apply the fix** - Fix the root cause
5. **Add safety guard** - Prevent negative values from displaying
6. **Test and verify** - Ensure the fix works

---

## Files to Monitor

- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` - API endpoint
- `backend/src/main/java/com/minet/sacco/entity/Loan.java` - Loan entity
- `minetsacco-main/src/pages/MemberDashboard.tsx` - Frontend display
- `minetsacco-main/src/config/api.ts` - API configuration
- `minetsacco-main/src/contexts/AuthContext.tsx` - Authentication

---

## Conclusion

The bug is 100% in the frontend or the API response. The database and backend are correct. Once we have the debug data, we'll know exactly where the bug is and how to fix it.
