# Quick Reference - Repayment Display Bug

## The Problem in One Sentence
Frontend shows **-80,000 KES and -40%** instead of **0 KES and 0%** for loans with no repayments.

## The Clue
The -80,000 is **exactly the interest amount** (80,000).

## What We Know
- ✓ Database is correct
- ✓ Backend API is correct
- ✓ Frontend code is mathematically correct
- ✗ Frontend is displaying wrong values

## What We Need
1. Console output showing [DEBUG] messages
2. Network response showing the JSON from the API

## Where the Bug Is
Either:
1. Backend is sending wrong data
2. API response is being intercepted and modified
3. Frontend is receiving different data than expected

## The Fix
1. Identify root cause using debug data
2. Fix the root cause
3. Add `Math.max(0, ...)` safety guard
4. Test and verify

## Key Files
- Frontend: `minetsacco-main/src/pages/MemberDashboard.tsx` (Lines 1117-1128)
- Backend: `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` (Line 254)
- API Config: `minetsacco-main/src/config/api.ts`

## Console.log Already in Place
Line 1117 in MemberDashboard.tsx already has debug logging. Just need to capture the output.

## Expected Console Output
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

## Expected Network Response
```json
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
```

## If Console Shows Negative Values
The frontend is receiving wrong data from the API.

## If Network Response Shows Wrong Values
The backend is sending wrong data.

## If Both Show Correct Values But Display is Wrong
The frontend calculation is wrong.

## Safety Guard to Add
```typescript
const repaidAmount = Math.max(0, loan.totalRepayable - loan.outstandingBalance);
const repaidPercentage = loan.totalRepayable > 0
  ? Math.max(0, Math.round((repaidAmount / loan.totalRepayable) * 100))
  : 0;
```

## Documentation Files
1. `COMPLETE_ISSUE_SUMMARY.md` - Full overview
2. `TECHNICAL_ANALYSIS_FOR_CLAUDE.md` - Technical details
3. `USER_ACTION_SUMMARY.md` - User instructions
4. `DEBUG_STEPS_FOR_USER.md` - Detailed debug steps
5. `CLAUDE_NEXT_ACTIONS.md` - Action plan
6. `QUICK_REFERENCE.md` - This file

## Next Step
Wait for user to provide debug data, then identify and fix the root cause.
