# Bug Root Cause Analysis - Repayment Display Bug

## The Evidence

From your screenshot:
- **Disbursed**: KES 200,000 ✓ (correct - this is `amount`)
- **Repaid**: KES -80,000 ✗ (WRONG - should be 0, but showing exactly the interest amount)
- **Outstanding**: KES 280,000 ✓ (correct - this is `totalRepayable`)

## The Math

```
Loan Amount: 200,000
Interest: 80,000
Total Repayable: 280,000

What's displayed:
Repaid = -80,000 (exactly the interest amount!)
```

## Code Path Analysis

### Backend - What It Should Send

**MemberPortalController.java (Line 254)**
```java
@GetMapping("/loans")
public ResponseEntity<?> getLoans() {
    Member member = getCurrentMember();
    List<Loan> loans = loanRepository.findByMemberId(member.getId());
    return ResponseEntity.ok(loans);  // Returns Loan entity
}
```

**Loan Entity Fields:**
- `amount` = 200,000
- `totalInterest` = 80,000
- `totalRepayable` = 280,000
- `outstandingBalance` = 280,000

**Expected JSON Response:**
```json
{
  "id": 13,
  "amount": 200000,
  "totalInterest": 80000,
  "totalRepayable": 280000,
  "outstandingBalance": 280000
}
```

### Frontend - What It Calculates

**MemberDashboard.tsx (Lines 1117-1128)**
```typescript
{formatCurrency(loan.totalRepayable - loan.outstandingBalance)}
// Should calculate: 280,000 - 280,000 = 0
```

**But it's displaying: -80,000**

## The Root Cause

The frontend is displaying `-80,000`, which is exactly `amount - totalRepayable`:
```
200,000 - 280,000 = -80,000 ✓
```

This means **one of these is true:**

### Possibility 1: Backend Sending Wrong outstandingBalance
The backend is sending `outstandingBalance = 360,000` instead of `280,000`
```
280,000 - 360,000 = -80,000 ✓
```

### Possibility 2: Frontend Using Wrong Field
The frontend is using `amount` instead of `totalRepayable` in the calculation
```
amount - totalRepayable = 200,000 - 280,000 = -80,000 ✓
```

### Possibility 3: API Response Being Modified
An interceptor or middleware is changing the values before they reach the frontend

## Code Verification

### Backend - Verified Correct ✓
- `LoanRepository.findByMemberId()` - Simple Spring Data query, no calculation
- `LoanService.getLoansByMemberId()` - Just returns repository result
- `Loan.java` - No custom JSON serialization
- `MemberPortalController.getLoans()` - Returns loans directly

### Frontend - Code Looks Correct ✓
```typescript
{formatCurrency(loan.totalRepayable - loan.outstandingBalance)}
```
This formula is mathematically correct.

## The Smoking Gun

**The -80,000 is exactly the interest amount.** This is not a coincidence.

The frontend is either:
1. Receiving `outstandingBalance = 360,000` from the backend (instead of 280,000)
2. Using a different field in the calculation than what the code shows
3. Receiving data that's been modified by an interceptor

## What Needs to Happen

To identify the exact root cause, we need to see:

1. **What the backend is actually sending** - Check the Network tab Response for `/member/loans`
2. **What the frontend is actually receiving** - Check the console.log output at line 1117

The console.log is already in place:
```typescript
console.log(`[DEBUG] Loan ${loan.id} (${loan.loanNumber}) - Raw API Data:`, {
  outstandingBalance: loan.outstandingBalance,
  calculatedRepaid: loan.totalRepayable - loan.outstandingBalance,
  calculatedPercentage: ((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100
});
```

## Next Step

**Log in as a member and check:**
1. Open Developer Tools (F12)
2. Go to Console tab
3. Look for `[DEBUG] Loan 13` message
4. Share the output

This will show exactly what values the frontend is receiving and calculating.
