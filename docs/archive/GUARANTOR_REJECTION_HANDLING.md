# Guarantor Rejection Handling - Implementation Guide

## Problem Statement

When a guarantor rejects a loan guarantee, the loan gets stuck in `PENDING_GUARANTOR_APPROVAL` status with no clear path forward. The borrower has no options, and the loan officer must manually intervene.

---

## Solution: 3 Valid Options for Borrower

When a guarantor rejects, the borrower can choose one of three actions:

### Option 1: Replace Guarantor

**When to use**: When you have another member who can guarantee the same amount.

**How it works**:
1. Borrower searches for a replacement guarantor by employee ID
2. System validates the new guarantor:
   - Has sufficient available savings
   - Is not already a guarantor for this loan
   - Is in ACTIVE status
3. New guarantor receives notification to approve/reject
4. If new guarantor approves, loan proceeds normally
5. If new guarantor rejects, borrower can try another replacement

**Example**:
```
Original loan: KES 15,000
Guarantors: Amina (5k), Brian (6k), Samuel (4k)

Brian rejects → Loan stuck

Borrower action: Replace Brian with Grace
Grace has 8,000 savings → ✓ Eligible to guarantee 6,000

New guarantors: Amina (5k), Grace (6k), Samuel (4k)
Loan proceeds when all approve
```

**When this is applicable**:
- ✅ Borrower knows another member who can guarantee
- ✅ Replacement member has sufficient savings
- ✅ Replacement member is willing to guarantee
- ✅ Loan amount stays the same
- ✅ No need to reduce loan or appeal

---

### Option 2: Reduce Loan Amount

**When to use**: When you want to proceed with a smaller loan amount that can be fully guaranteed by remaining guarantors.

**How it works**:
1. System calculates total guarantee from non-rejecting guarantors
2. Borrower can reduce loan amount to match or be less than total guarantee
3. Interest and monthly repayment are recalculated
4. Loan is sent back to Credit Committee for re-approval
5. If committee approves, loan proceeds with new amount

**Example**:
```
Original loan: KES 15,000
Guarantors: Amina (5k), Brian (6k), Samuel (4k)
Total guarantee: 15,000

Brian rejects → Loan stuck

Remaining guarantors: Amina (5k), Samuel (4k)
Remaining guarantee: 9,000

Borrower action: Reduce loan to KES 9,000
New interest: 900 (instead of 1,500)
New monthly payment: 750 (instead of 1,250)

Loan sent to Credit Committee for re-approval
Committee approves → Loan proceeds with 9,000
```

**When this is applicable**:
- ✅ Borrower can proceed with smaller amount
- ✅ Remaining guarantors can cover reduced amount
- ✅ Borrower doesn't know replacement guarantor
- ✅ Borrower prefers smaller loan over finding new guarantor
- ✅ Loan amount is flexible

---

### Option 3: Withdraw Application

**When to use**: When you want to cancel this loan application and reapply later with different guarantors.

**How it works**:
1. Borrower clicks "Withdraw Application"
2. System confirms the action
3. Loan status changes to REJECTED
4. All guarantors are notified
5. Borrower can reapply later with different guarantors
6. No financial impact (no money disbursed yet)

**Example**:
```
Original loan: KES 15,000
Guarantors: Amina (5k), Brian (6k), Samuel (4k)

Brian rejects → Loan stuck

Borrower action: Withdraw application
Reason: "Will reapply with different guarantors"

Loan status: REJECTED
All guarantors notified
Borrower can reapply anytime with new guarantors
```

**When this is applicable**:
- ✅ Borrower doesn't have replacement guarantor
- ✅ Borrower doesn't want to reduce loan amount
- ✅ Borrower wants to try again later
- ✅ Borrower wants to find better guarantors
- ✅ No financial commitment yet (loan not disbursed)

---

## Database Changes

### New Loan Status
```java
public enum Status {
    PENDING,
    PENDING_GUARANTOR_APPROVAL,
    PENDING_GUARANTOR_REPLACEMENT,  // ← NEW
    PENDING_LOAN_OFFICER_REVIEW,
    PENDING_CREDIT_COMMITTEE,
    PENDING_TREASURER,
    APPROVED,
    REJECTED,
    DISBURSED,
    REPAID,
    DEFAULTED,
    WRITTEN_OFF
}
```

### New Guarantor Status
```java
public enum Status {
    PENDING,
    ACCEPTED,
    REJECTED,
    REPLACED,  // ← NEW
    ACTIVE,
    DECLINED,
    RELEASED
}
```

### New Columns
```sql
ALTER TABLE loans ADD COLUMN original_amount DECIMAL(19,2);
```

---

## Implementation Checklist

- [ ] Add `PENDING_GUARANTOR_REPLACEMENT` status to Loan entity
- [ ] Add `REPLACED` status to Guarantor entity
- [ ] Add `original_amount` column to loans table
- [ ] Implement `replaceGuarantor()` endpoint
- [ ] Implement `reduceLoanAmount()` endpoint
- [ ] Implement `withdrawLoanApplication()` endpoint
- [ ] Update guarantor rejection logic to set loan status
- [ ] Create borrower notification dialog
- [ ] Add Replace Guarantor UI
- [ ] Add Reduce Amount UI
- [ ] Add Withdraw Application UI
- [ ] Test all three workflows
- [ ] Update Loan Officer dashboard
- [ ] Deploy and monitor

