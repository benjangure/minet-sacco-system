# Guarantor Reassignment on Loan Amount Reduction

## Overview

When a member reduces their loan amount after a guarantor rejection, all guarantor assignments must be **cleared and reset**. The member must then **re-assign guarantors** with new guarantee amounts that match the reduced loan amount.

## Problem Statement

**Current Behavior (WRONG):**
```
Initial Loan: 10,000 KES
├─ Brian: 5,000 (PENDING)
└─ Amina: 5,000 (REJECTED)

Samuel reduces to: 8,000 KES
├─ Brian: 5,000 (UNCHANGED) ← WRONG
└─ Amina: 5,000 (UNCHANGED) ← WRONG
```

**Issue:** Guarantor amounts don't match the new loan amount, creating confusion about who guarantees what.

**Correct Behavior:**
```
Initial Loan: 10,000 KES
├─ Brian: 5,000 (PENDING)
└─ Amina: 5,000 (REJECTED)

Samuel reduces to: 8,000 KES
    ↓ (Guarantors CLEARED)
├─ Brian: 0 (RESET)
└─ Amina: 0 (RESET)
    ↓ (Member RE-ASSIGNS)
├─ Brian: 4,000 (NEW)
└─ Amina: 4,000 (NEW)
    ↓ (Guarantors RE-APPROVE)
├─ Brian: ACCEPTED
└─ Amina: ACCEPTED
```

## Solution Design

### Step 1: Clear Guarantor Assignments
When loan amount is reduced:
- Set all guarantor amounts to 0
- Mark guarantors as PENDING_REASSIGNMENT
- Store previous amounts for audit trail

### Step 2: Notify All Stakeholders
- **Guarantors:** "Loan amount changed. Your previous guarantee is no longer valid. Please re-assign."
- **Member:** "Loan reduced. Please re-assign guarantor amounts."
- **Credit Committee:** "Loan reduced and pending guarantor reassignment."

### Step 3: Member Re-assigns Guarantors
- Member selects guarantors again
- Member specifies new guarantee amounts
- System validates total guarantees cover new loan amount

### Step 4: Guarantors Re-approve
- Each guarantor receives new approval request
- Guarantor can accept, reduce, or reject
- System waits for all responses

### Step 5: Credit Committee Reviews
- Reviews loan with new guarantor assignments
- Ensures total guarantees cover new amount
- Approves or rejects

## Implementation Details

### Database Changes
```sql
-- Add new status to Guarantor enum
ALTER TABLE guarantors MODIFY COLUMN status ENUM(
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'REPLACED',
    'ACTIVE',
    'DECLINED',
    'RELEASED',
    'PENDING_REASSIGNMENT'  -- NEW
) DEFAULT 'PENDING';

-- Track previous guarantee amount
ALTER TABLE guarantors ADD COLUMN previous_guarantee_amount DECIMAL(19,2);
ALTER TABLE guarantors ADD COLUMN reassignment_reason VARCHAR(255);
```

### Backend Changes

**LoanService.reduceLoanAmount():**
1. Clear all guarantor amounts
2. Mark guarantors as PENDING_REASSIGNMENT
3. Store previous amounts
4. Update loan status to PENDING_GUARANTOR_REASSIGNMENT
5. Notify guarantors and member
6. Audit log the change

**New Endpoint: POST /loans/{loanId}/reassign-guarantors**
- Accept new guarantor assignments
- Validate total covers new amount
- Create new guarantor approval requests
- Update loan status to PENDING_GUARANTOR_APPROVAL

### Frontend Changes

**New Component: GuarantorReassignmentDialog**
- Shows old guarantor assignments (read-only)
- Shows new loan amount
- Form to select and assign new guarantors
- Validation that total covers new amount
- Submit button to save new assignments

**Update MemberDashboard:**
- Detect PENDING_GUARANTOR_REASSIGNMENT status
- Show "Reassign Guarantors" button
- Open GuarantorReassignmentDialog

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

## Audit Trail

All changes logged:
- Original guarantor amounts
- New guarantor amounts
- Reason for change
- Notifications sent
- Guarantor responses
- Timestamps
- Who made changes

## What This Achieves

✅ **Clear Audit Trail:** Every guarantor change is logged with reason and timestamp
✅ **Explicit Guarantor Approval:** Guarantors explicitly approve new amounts
✅ **No Confusion:** Everyone knows exactly who guarantees what
✅ **SACCO Compliance:** Matches real SACCO best practices
✅ **Transparent Process:** All parties notified and involved
✅ **Risk Management:** Ensures guarantees always cover loan amount
✅ **Flexibility:** Members can adjust guarantor mix when loan amount changes

## Example Workflow

**Initial:** Samuel applies for 10,000 KES with Brian (5k) and Amina (5k)
**Amina rejects:** Loan status = PENDING_GUARANTOR_REPLACEMENT
**Samuel reduces to 8,000:** 
- Brian: 5,000 → 0 (RESET)
- Amina: 5,000 → 0 (RESET)
- Status: PENDING_GUARANTOR_REASSIGNMENT
- Notifications sent

**Samuel re-assigns:**
- Brian: 4,000
- Amina: 4,000
- Total: 8,000 ✓
- Status: PENDING_GUARANTOR_APPROVAL

**Guarantors approve:**
- Brian: ACCEPTED
- Amina: ACCEPTED
- Status: PENDING_CREDIT_COMMITTEE

**Credit Committee reviews:**
- Loan: 8,000
- Guarantees: 8,000 ✓
- Status: APPROVED

## Files to Modify

### Backend
1. `Loan.java` - Add PENDING_GUARANTOR_REASSIGNMENT status
2. `Guarantor.java` - Add PENDING_REASSIGNMENT status, previous_guarantee_amount field
3. `LoanService.java` - Modify reduceLoanAmount() to clear guarantors
4. `LoanService.java` - Add reassignGuarantors() method
5. `LoanController.java` - Add /reassign-guarantors endpoint
6. Database migration - Add new columns and status values

### Frontend
1. `GuarantorReassignmentDialog.tsx` - New component
2. `MemberDashboard.tsx` - Detect PENDING_GUARANTOR_REASSIGNMENT and show button
3. `MemberLoanApplication.tsx` - Update guarantor selection logic

## Status

**Current:** ❌ NOT IMPLEMENTED
**Priority:** HIGH
**Complexity:** MEDIUM
**Impact:** HIGH (improves clarity and compliance)
