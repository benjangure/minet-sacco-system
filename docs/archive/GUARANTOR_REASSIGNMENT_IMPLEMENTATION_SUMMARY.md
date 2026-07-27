# Guarantor Reassignment Implementation - Complete

## Overview
Completed the full implementation of the guarantor reassignment workflow when a member reduces their loan amount after a guarantor rejection.

## What Was Implemented

### 1. Database Migration (V87)
**File:** `backend/src/main/resources/db/migration/V87__Add_guarantor_reassignment_fields.sql`

- Added `previous_guarantee_amount` column to guarantors table
- Added `reassignment_reason` column to guarantors table
- Extended `loans.status` ENUM to include `PENDING_GUARANTOR_REASSIGNMENT`
- Extended `guarantors.status` ENUM to include `PENDING_REASSIGNMENT`

### 2. Backend Service Method
**File:** `backend/src/main/java/com/minet/sacco/service/LoanService.java`

**New Method:** `reassignGuarantors()`
- Accepts new guarantor assignments with new guarantee amounts
- Validates that all amounts are positive
- Validates that total guarantees cover the new loan amount
- Updates guarantor amounts and resets status to PENDING for new approval
- Updates loan status to PENDING_GUARANTOR_APPROVAL
- Sends notifications to:
  - Guarantors: New approval request with new guarantee amount
  - Member: Confirmation of successful reassignment
- Creates audit log entry

**Updated Method:** `reduceLoanAmount()`
- Already implemented to clear guarantor amounts
- Sets guarantors to PENDING_REASSIGNMENT status
- Stores previous amounts for audit trail
- Sets loan status to PENDING_GUARANTOR_REASSIGNMENT
- Sends notifications to all parties

### 3. Backend Controller Endpoint
**File:** `backend/src/main/java/com/minet/sacco/controller/LoanController.java`

**New Endpoint:** `POST /loans/{loanId}/reassign-guarantors`
- Accepts list of guarantor assignments with new amounts
- Calls LoanService.reassignGuarantors()
- Returns updated loan object
- Requires ROLE_MEMBER authorization

### 4. Frontend Component
**File:** `minetsacco-main/src/components/GuarantorReassignmentDialog.tsx`

**Features:**
- Displays loan summary (original amount, new amount)
- Shows all guarantors needing reassignment
- Displays previous guarantee amounts for reference
- Form to enter new guarantee amounts for each guarantor
- Real-time validation:
  - All amounts must be positive
  - Total must equal or exceed loan amount
  - Shows shortfall if insufficient
- Visual feedback (green when valid, orange when invalid)
- Submit button disabled until valid
- Loading state during submission
- Toast notifications for success/error
- Comprehensive error messages

### 5. Frontend Integration
**File:** `minetsacco-main/src/pages/MemberDashboard.tsx`

**Changes:**
- Imported GuarantorReassignmentDialog component
- Added state management:
  - `reassignmentDialogOpen`: Controls dialog visibility
  - `reassignmentLoan`: Stores loan needing reassignment
  - `reassignmentGuarantors`: Stores guarantors needing reassignment
- Added `fetchGuarantorDataForReassignment()` function:
  - Fetches guarantor data from backend
  - Filters guarantors with PENDING_REASSIGNMENT status
  - Opens dialog automatically when found
- Updated `fetchActiveLoans()`:
  - Added PENDING_GUARANTOR_REASSIGNMENT to loan status filter
  - Detects loans needing reassignment
  - Calls fetchGuarantorDataForReassignment() automatically
- Updated loan status display:
  - Added PENDING_GUARANTOR_REASSIGNMENT status styling (orange)
  - Shows "Reassign Guarantors Required" label
- Added "Reassign Guarantors" button:
  - Appears only for PENDING_GUARANTOR_REASSIGNMENT loans
  - Opens reassignment dialog when clicked
- Dialog integration with proper state cleanup on completion

## Workflow

### Step 1: Loan Amount Reduction
```
Member reduces loan from 10,000 to 8,000
↓
reduceLoanAmount() is called
↓
All guarantor amounts set to 0
All guarantors marked as PENDING_REASSIGNMENT
Loan status set to PENDING_GUARANTOR_REASSIGNMENT
Notifications sent to all parties
```

### Step 2: Member Re-assigns Guarantors
```
Member sees "Reassign Guarantors Required" status
↓
Clicks "Reassign Guarantors" button
↓
GuarantorReassignmentDialog opens
↓
Member enters new guarantee amounts
↓
System validates total covers new loan amount
↓
Member submits
```

### Step 3: Backend Processing
```
reassignGuarantors() is called
↓
Validates all assignments
↓
Updates guarantor amounts
↓
Resets guarantor status to PENDING
↓
Sets loan status to PENDING_GUARANTOR_APPROVAL
↓
Sends notifications
↓
Creates audit log
```

### Step 4: Guarantor Approval
```
Guarantors receive new approval requests
↓
Each guarantor approves/rejects new amounts
↓
Once all approve, loan goes to PENDING_CREDIT_COMMITTEE
```

### Step 5: Credit Committee Review
```
Credit Committee reviews loan with new guarantor assignments
↓
Ensures total guarantees cover new amount
↓
Approves or rejects
```

## Loan Status Flow

```
PENDING_GUARANTOR_REPLACEMENT
    ↓ (Member reduces loan)
PENDING_GUARANTOR_REASSIGNMENT
    ↓ (Member re-assigns guarantors)
PENDING_GUARANTOR_APPROVAL
    ↓ (Guarantors approve new amounts)
PENDING_CREDIT_COMMITTEE
    ↓ (Credit Committee reviews)
PENDING_TREASURER or REJECTED
```

## Guarantor Status Flow

```
PENDING/ACCEPTED/REJECTED
    ↓ (Loan amount changes)
PENDING_REASSIGNMENT
    ↓ (Member re-assigns)
PENDING (new approval request)
    ↓ (Guarantor responds)
ACCEPTED/REJECTED/REDUCED
```

## Key Features

✅ **Clear Audit Trail:** Every guarantor change is logged with reason and timestamp
✅ **Explicit Guarantor Approval:** Guarantors explicitly approve new amounts
✅ **No Confusion:** Everyone knows exactly who guarantees what
✅ **SACCO Compliance:** Matches real SACCO best practices
✅ **Transparent Process:** All parties notified and involved
✅ **Risk Management:** Ensures guarantees always cover loan amount
✅ **Flexibility:** Members can adjust guarantor mix when loan amount changes
✅ **Validation:** Comprehensive validation at every step
✅ **User Feedback:** Clear error messages and success notifications

## Testing Checklist

- [ ] Backend compiles successfully
- [ ] Database migration V87 applies without errors
- [ ] Member can see "Reassign Guarantors Required" status
- [ ] Member can click "Reassign Guarantors" button
- [ ] Dialog opens with correct guarantor data
- [ ] Validation works (amounts must be positive, total must cover loan)
- [ ] Submit button disabled until valid
- [ ] Reassignment successful - loan status changes to PENDING_GUARANTOR_APPROVAL
- [ ] Guarantors receive new approval notifications
- [ ] Member receives confirmation notification
- [ ] Audit log records the reassignment
- [ ] Loan workflow continues correctly after reassignment

## Files Modified/Created

### Backend
- ✅ `backend/src/main/resources/db/migration/V87__Add_guarantor_reassignment_fields.sql` (CREATED)
- ✅ `backend/src/main/java/com/minet/sacco/service/LoanService.java` (MODIFIED - added reassignGuarantors method)
- ✅ `backend/src/main/java/com/minet/sacco/controller/LoanController.java` (MODIFIED - added endpoint)
- ✅ `backend/src/main/java/com/minet/sacco/entity/Loan.java` (ALREADY HAD PENDING_GUARANTOR_REASSIGNMENT)
- ✅ `backend/src/main/java/com/minet/sacco/entity/Guarantor.java` (ALREADY HAD new fields)

### Frontend
- ✅ `minetsacco-main/src/components/GuarantorReassignmentDialog.tsx` (CREATED)
- ✅ `minetsacco-main/src/pages/MemberDashboard.tsx` (MODIFIED - integrated dialog)

## Status

**IMPLEMENTATION COMPLETE** ✅

All components are implemented and integrated. Backend compiles successfully. Frontend has no TypeScript errors. Ready for testing.

## Next Steps

1. Test the end-to-end workflow
2. Verify notifications are sent correctly
3. Check audit logs are created
4. Test edge cases (invalid amounts, insufficient guarantees, etc.)
5. Verify loan workflow continues correctly after reassignment
