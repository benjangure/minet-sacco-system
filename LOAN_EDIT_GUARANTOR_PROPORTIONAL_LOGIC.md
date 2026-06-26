# Loan Edit with Proportional Pledge Reduction Logic

## Overview
Implemented comprehensive loan editing feature with advanced guarantor management including proportional pledge reduction based on outstanding balance vs principal.

## Key Principle: Proportional Pledge Calculation

When editing a loan with guarantor information, pledges are calculated proportionally to the outstanding balance:

```
Actual Pledge = Guarantee Amount × (Outstanding Balance / Principal)
```

### Example
- **Original Loan**: KES 100,000 with 2 guarantors (50k each)
- **After Repayment**: Outstanding = KES 75,000 (25% repaid)
- **When Reassigning Guarantors**:
  - User enters: 50k + 50k (original guarantee amounts)
  - System calculates proportion: 75,000 / 100,000 = 0.75
  - Actual pledges frozen: 50,000 × 0.75 = **37,500 each**
  - Total frozen: **75,000** (matches outstanding balance)

## Components

### 1. Backend: LoanService.updateLoan()
**Location**: `backend/src/main/java/com/minet/sacco/service/LoanService.java`

**Key Changes**:
- Validates total guarantee amount ≥ outstanding balance
- Calculates reduction ratio: `outstandingBalance / principal`
- Applies proportional pledge: `guaranteeAmount × reductionRatio`
- Unfreezes old guarantor savings and freezes new ones proportionally

**Validation Rules**:
- Total new guarantees must be ≥ outstanding balance (or principal if outstanding is 0)
- All guarantors must be ACTIVE members
- All guarantors must have sufficient savings for their guarantee amount
- Guarantorship type must be "NORMAL" or "SELF"

**Example Logic Flow**:
```java
BigDecimal reductionRatio = outstandingBalance.divide(principal, 10, HALF_UP);
BigDecimal actualPledge = guaranteeAmount.multiply(reductionRatio).setScale(2, HALF_UP);
guarantor.setGuaranteeAmount(guaranteeAmount);  // Original amount stored
guarantor.setPledgeAmount(actualPledge);         // Proportional amount frozen
```

### 2. Frontend: Loans Edit Dialog
**Location**: `minetsacco-main/src/pages/Loans.tsx`

**New Features**:
- **Current Guarantors Display**: Shows all current guarantors with:
  - Name, member number, employee ID
  - Guarantee amount and current pledge (frozen amount)
  - Status
  - Proportional recalculation preview
  
- **Proportional Pledge Preview**: When entering new guarantors, displays:
  - "Will freeze: KES 37,500 (75% of KES 50,000)"
  - Real-time calculation as outstanding balance changes
  
- **Outstanding Balance Integration**: 
  - Edit dialog shows current outstanding balance
  - Recalculation percentage updates when outstanding is edited

**Form Fields**:
```
1. Disbursement Date (optional)
2. Outstanding Balance (optional)
3. Term Months (optional)
4. Reassign Guarantors (optional)
   - Employee ID
   - Guarantee Amount
   - System calculates: "Will freeze: KES X (Y% of KES Z)"
```

### 3. Backend Endpoint
**Endpoint**: `PUT /api/loans/{loanId}/update`

**Request Body**:
```json
{
  "disbursementDate": "2024-06-15",
  "outstandingBalance": 75000,
  "termMonths": 12,
  "guarantorshipType": "NORMAL",
  "guarantors": [
    {
      "employeeId": "EMP001",
      "pledgeAmount": 50000
    },
    {
      "employeeId": "EMP002",
      "pledgeAmount": 50000
    }
  ]
}
```

**Processing**:
1. Validates outstanding balance ≤ principal
2. Validates total guarantees ≥ outstanding balance
3. Calculates proportion: 75,000 / 100,000 = 0.75
4. For each guarantor:
   - Stores original guarantee: 50,000
   - Freezes proportional pledge: 50,000 × 0.75 = 37,500
5. Unfreezes old guarantor savings
6. Freezes new guarantor savings (proportional amount)

**Response**:
```json
{
  "success": true,
  "message": "Loan updated successfully",
  "data": {
    "id": 1,
    "loanNumber": "LN-001",
    "status": "DISBURSED",
    "disbursementDate": "2024-06-15",
    "outstandingBalance": 75000,
    "termMonths": 12
  }
}
```

## Database Impact

### Guarantor Table Changes
When guarantors are reassigned:
- Old guarantor records deleted
- New guarantor records created with:
  - `guaranteeAmount`: Original amount (50,000)
  - `pledgeAmount`: Proportional amount (37,500)
  - `migrationStatus`: "UI_UPDATED"
  - `status`: ACTIVE (if loan is DISBURSED) or RELEASED (otherwise)

### Account Table Changes
- Old guarantor: `frozenSavings` decremented by old pledge amount
- New guarantor: `frozenSavings` incremented by **proportional pledge amount**

**Example**:
```
Member A:
  Before: frozenSavings = 50,000
  After:  frozenSavings = 37,500  (50,000 × 75%)

Member B (new):
  Before: frozenSavings = 0
  After:  frozenSavings = 37,500  (50,000 × 75%)
```

## Loan Migration Integration

The same proportional logic applies to loan migrations:

When migrating a loan with:
- Principal: 100,000
- Outstanding: 75,000
- User enters guarantors: 50k + 50k

System will:
1. Calculate ratio: 75,000 / 100,000 = 0.75
2. Freeze: 37,500 each (total 75,000)
3. Store guarantee: 50,000 each

## Validation & Error Handling

**Validation Errors Thrown**:
1. "Loan can only be edited in DISBURSED, REPAID, or DEFAULTED status"
2. "At least one field must be updated"
3. "Disbursement date cannot be in the future"
4. "Outstanding balance must be >= 0"
5. "Outstanding balance cannot exceed principal"
6. "Outstanding balance must be 0 for REPAID loans"
7. "Term months must be > 0"
8. "Total guarantee amount must be at least equal to outstanding balance"
9. "Guarantor not found: {employeeId}"
10. "Guarantor {employeeId} is not ACTIVE"
11. "Guarantor {employeeId} has insufficient savings"
12. "Invalid guarantorship type"

## Status Handling

**Loan Status-Specific Behavior**:

| Status | Pledges Frozen | Behavior |
|--------|---|---|
| DISBURSED | Yes (proportional) | Freeze calculated pledges |
| REPAID | No | Set pledges to 0, guarantors RELEASED |
| DEFAULTED | No | Set pledges to 0, guarantors RELEASED |

## Testing Scenarios

### Scenario 1: Proportional Reduction
```
Loan: 100k, Outstanding: 75k, 2 guarantors
Input: 50k + 50k
Expected: 37.5k + 37.5k frozen
```

### Scenario 2: Insufficient Guarantees
```
Loan: 100k, Outstanding: 75k, 2 guarantors
Input: 30k + 30k (total 60k < 75k required)
Expected: Error - "Total guarantee amount must be at least equal to outstanding balance"
```

### Scenario 3: Outstanding = 0
```
Loan: 100k, Outstanding: 0k
Input: 50k + 50k
Expected: 0 frozen (loan fully repaid)
```

## API Permissions

**Required Role**: `ROLE_ADMIN`, `ROLE_TREASURER`, or `ROLE_LOAN_OFFICER`

## Files Modified/Created

### Backend
- `LoanService.java` - Updated `updateLoan()` method with proportional logic
- `LoanController.java` - Updated `/update` endpoint

### Frontend
- `Loans.tsx` - Enhanced Edit dialog with current guarantors display and proportional calculations

## Future Enhancements

1. **Audit Trail**: Log who changed guarantors and what the changes were
2. **Notification**: Notify affected guarantors when reassigned
3. **Batch Operations**: Edit multiple loans simultaneously
4. **Approval Workflow**: Optional approval step before changes take effect
5. **Historical Records**: Keep audit trail of all guarantee changes
